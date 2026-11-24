# X-Ray Cam (Android)

Aplikasi kamera Android dengan **efek X-ray yang tingkatnya bisa diatur**.

> ⚠️ HP tidak bisa memancarkan sinar-X. Aplikasi ini membuat **simulasi visual**
> dari gambar kamera (grayscale → negatif → kontras → pewarnaan), bukan pemindaian
> tembus pandang sungguhan.

## Fitur
- Kamera langsung (belakang/depan) dengan efek X-ray secara real-time (CameraX).
- **Tingkat Ray** 0–100% — seberapa kuat efek menutupi gambar asli.
- **Daya Tembus** — makin tinggi, bagian "lunak" hilang dan hanya bagian padat yang terang.
- **Kontras** 50–300% dan **Cahaya (Glow)** pada bagian terang.
- **Warna ray**: Biru Medis, Putih Klasik, Hijau, Ungu, Amber.
- Preset cepat: Rendah / Sedang / Tinggi / Maks, dan tombol Reset.
- Efek garis pindai (scanline) animasi, bisa dimatikan.
- Buka foto dari galeri lalu terapkan efek yang sama.
- Simpan hasil ke Galeri (`Pictures/XRayCam`).
- Pengaturan tersimpan otomatis.

## Struktur
```
app/src/main/java/com/tugaskampus/xray/
├── MainActivity.kt    # UI, kamera, galeri, simpan foto
├── XRayProcessor.kt   # pengolahan gambar (ColorMatrix + glow)
├── XRaySettings.kt    # parameter, preset, penyimpanan pengaturan
└── ScanlineView.kt    # overlay animasi garis pindai
```

## Cara build
**Android Studio:** buka folder `XRayApp`, tunggu Gradle sync, lalu Run ▶ ke HP
(aktifkan *USB debugging*). Minimal Android 8.0 (API 26).

**Command line:**
```bash
cd XRayApp
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

**GitHub Actions:** setiap push ke folder `XRayApp/` otomatis membangun APK.
Unduh dari tab *Actions* → run terbaru → artifact `XRayCam-apk`.
