package com.tugaskampus.xray

import android.content.Context

/** Warna "sinar" yang dipakai untuk mewarnai hasil X-ray (faktor pengali R, G, B). */
enum class RayTint(val label: String, val r: Float, val g: Float, val b: Float) {
    MEDICAL_BLUE("Biru Medis", 0.72f, 0.90f, 1.00f),
    CLASSIC_WHITE("Putih Klasik", 1.00f, 1.00f, 1.00f),
    NIGHT_GREEN("Hijau", 0.55f, 1.00f, 0.60f),
    PURPLE("Ungu", 0.85f, 0.65f, 1.00f),
    AMBER("Amber", 1.00f, 0.78f, 0.40f),
}

/**
 * Semua parameter yang bisa dikustomisasi pengguna.
 *
 * @param intensity   Tingkat ray 0..100 — seberapa kuat efek X-ray menutupi gambar asli.
 * @param penetration Daya tembus 0..100 — makin tinggi, bagian "lunak" makin hilang
 *                    sehingga hanya bagian paling padat yang tampak terang.
 * @param contrast    Kontras dalam persen (50..300).
 * @param glow        Kekuatan pendar/cahaya pada bagian terang 0..100.
 */
data class XRaySettings(
    val intensity: Int = 70,
    val penetration: Int = 40,
    val contrast: Int = 140,
    val glow: Int = 30,
    val tint: RayTint = RayTint.MEDICAL_BLUE,
    val scanline: Boolean = true,
) {
    companion object {
        private const val PREFS = "xray_settings"

        val PRESET_LOW = XRaySettings(intensity = 35, penetration = 15, contrast = 110, glow = 10)
        val PRESET_MID = XRaySettings(intensity = 70, penetration = 40, contrast = 140, glow = 30)
        val PRESET_HIGH = XRaySettings(intensity = 90, penetration = 65, contrast = 190, glow = 55)
        val PRESET_MAX = XRaySettings(intensity = 100, penetration = 90, contrast = 260, glow = 85)

        fun load(context: Context): XRaySettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val d = XRaySettings()
            return XRaySettings(
                intensity = p.getInt("intensity", d.intensity),
                penetration = p.getInt("penetration", d.penetration),
                contrast = p.getInt("contrast", d.contrast),
                glow = p.getInt("glow", d.glow),
                tint = runCatching { RayTint.valueOf(p.getString("tint", d.tint.name)!!) }
                    .getOrDefault(d.tint),
                scanline = p.getBoolean("scanline", d.scanline),
            )
        }
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("intensity", intensity)
            .putInt("penetration", penetration)
            .putInt("contrast", contrast)
            .putInt("glow", glow)
            .putString("tint", tint.name)
            .putBoolean("scanline", scanline)
            .apply()
    }

    /** Label tingkat yang ditampilkan di layar. */
    fun levelName(): String = when {
        intensity == 0 -> "MATI"
        intensity < 40 -> "RENDAH"
        intensity < 75 -> "SEDANG"
        intensity < 95 -> "TINGGI"
        else -> "MAKS"
    }
}
