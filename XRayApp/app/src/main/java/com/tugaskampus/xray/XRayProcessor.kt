package com.tugaskampus.xray

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Mengubah gambar biasa menjadi tampilan bergaya X-ray.
 *
 * Langkahnya:
 *  1. grayscale  → 2. negatif (invert) → 3. kontras → 4. daya tembus (membuang
 *  bagian "lunak") → 5. pewarnaan sesuai warna ray → 6. glow pada bagian terang.
 * Hasil X-ray kemudian dicampur dengan gambar asli sesuai "Tingkat Ray".
 *
 * Tidak thread-safe: panggil dari satu thread saja.
 */
class XRayProcessor {

    // Triple buffer supaya bitmap yang sedang ditampilkan tidak ditimpa saat diproses.
    private val buffers = arrayOfNulls<Bitmap>(3)
    private var bufferIndex = 0
    private var glowBitmap: Bitmap? = null

    private val plainPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val xrayPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val glowSrcPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val glowPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    private var cachedSettings: XRaySettings? = null

    /**
     * @param rotationDegrees rotasi yang perlu diterapkan agar gambar tegak.
     * @param mirror          true untuk kamera depan (efek cermin).
     * @return bitmap hasil (milik processor, jangan di-recycle).
     */
    fun process(
        src: Bitmap,
        rotationDegrees: Int,
        mirror: Boolean,
        settings: XRaySettings,
    ): Bitmap {
        updateFilters(settings)

        // Matriks transformasi (rotasi + cermin) dan ukuran output.
        val m = Matrix()
        m.postRotate(rotationDegrees.toFloat())
        if (mirror) m.postScale(-1f, 1f)
        val bounds = RectF(0f, 0f, src.width.toFloat(), src.height.toFloat())
        m.mapRect(bounds)
        m.postTranslate(-bounds.left, -bounds.top)
        val outW = bounds.width().roundToInt()
        val outH = bounds.height().roundToInt()

        bufferIndex = (bufferIndex + 1) % buffers.size
        var out = buffers[bufferIndex]
        if (out == null || out.width != outW || out.height != outH) {
            out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            buffers[bufferIndex] = out
        }

        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)

        val intensity = settings.intensity.coerceIn(0, 100)
        if (intensity < 100) {
            canvas.drawBitmap(src, m, plainPaint)
        }
        if (intensity > 0) {
            xrayPaint.alpha = intensity * 255 / 100
            canvas.drawBitmap(src, m, xrayPaint)

            if (settings.glow > 0) drawGlow(canvas, src, m, outW, outH, settings, intensity)
        }
        return out
    }

    /** Glow murah: render kecil (≈ blur) lalu diperbesar dan dijumlahkan (ADD). */
    private fun drawGlow(
        canvas: Canvas,
        src: Bitmap,
        m: Matrix,
        outW: Int,
        outH: Int,
        settings: XRaySettings,
        intensity: Int,
    ) {
        val scale = 1f / GLOW_DOWNSCALE
        val gw = max(1, (outW * scale).roundToInt())
        val gh = max(1, (outH * scale).roundToInt())
        var g = glowBitmap
        if (g == null || g.width != gw || g.height != gh) {
            g = Bitmap.createBitmap(gw, gh, Bitmap.Config.ARGB_8888)
            glowBitmap = g
        }
        val gc = Canvas(g)
        gc.drawColor(Color.BLACK)
        val gm = Matrix(m).apply { postScale(scale, scale) }
        gc.drawBitmap(src, gm, glowSrcPaint)

        glowPaint.alpha = (settings.glow * intensity * 255 / 10_000).coerceIn(0, 255)
        canvas.drawBitmap(g, null, Rect(0, 0, outW, outH), glowPaint)
    }

    private fun updateFilters(s: XRaySettings) {
        if (s == cachedSettings) return
        cachedSettings = s

        val xray = buildXRayMatrix(s)
        xrayPaint.colorFilter = ColorMatrixColorFilter(xray)

        // Untuk glow: hanya bagian paling terang yang dipertahankan.
        val glow = ColorMatrix(xray).apply {
            postConcat(scaleOffset(2.2f, -170f))
        }
        glowSrcPaint.colorFilter = ColorMatrixColorFilter(glow)
    }

    companion object {
        private const val GLOW_DOWNSCALE = 10f

        fun buildXRayMatrix(s: XRaySettings): ColorMatrix {
            val result = ColorMatrix().apply { setSaturation(0f) }   // 1. grayscale

            result.postConcat(                                          // 2. invert
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f,
                    )
                )
            )

            val c = s.contrast / 100f                                   // 3. kontras
            result.postConcat(scaleOffset(c, 128f * (1f - c)))

            val p = s.penetration / 100f                                // 4. daya tembus
            result.postConcat(scaleOffset(1f + p * 0.9f, -p * 150f))

            result.postConcat(                                          // 5. warna ray
                ColorMatrix().apply { setScale(s.tint.r, s.tint.g, s.tint.b, 1f) }
            )
            return result
        }

        private fun scaleOffset(scale: Float, offset: Float) = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, offset,
                0f, scale, 0f, 0f, offset,
                0f, 0f, scale, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }
}
