package com.tugaskampus.xray

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/** Overlay garis pindai yang bergerak naik-turun, seperti mesin pemindai. */
class ScanlineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint().apply { strokeWidth = 1f }
    private var progress = 0f
    private var color = Color.rgb(79, 195, 247)

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2600
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    var enabledEffect: Boolean = true
        set(value) {
            field = value
            visibility = if (value) VISIBLE else GONE
            updateAnimator()
        }

    fun setTint(tint: RayTint) {
        color = Color.rgb((tint.r * 255).toInt(), (tint.g * 255).toInt(), (tint.b * 255).toInt())
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimator()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    private fun updateAnimator() {
        if (enabledEffect && isAttachedToWindow) {
            if (!animator.isStarted) animator.start()
        } else {
            animator.cancel()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        // Garis-garis tipis horizontal (efek monitor).
        linePaint.color = Color.argb(28, Color.red(color), Color.green(color), Color.blue(color))
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w, y, linePaint)
            y += 6f
        }

        // Pita cahaya yang bergerak.
        val bandH = h * 0.12f
        val center = progress * h
        val transparent = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))
        val bright = Color.argb(110, Color.red(color), Color.green(color), Color.blue(color))
        bandPaint.shader = LinearGradient(
            0f, center - bandH, 0f, center + bandH,
            intArrayOf(transparent, bright, transparent), floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, center - bandH, w, center + bandH, bandPaint)
    }
}
