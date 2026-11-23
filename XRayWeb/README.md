# X-Ray Cam Web

Versi web dari X-Ray Cam: satu file `index.html`, tanpa instalasi atau build.

> ⚠️ Simulasi efek visual, bukan sinar-X sungguhan.

## Fitur
- Kamera langsung (depan/belakang) atau foto dari galeri/kamera HP.
- Tingkat Ray, Daya Tembus (dengan perkiraan kVp), Kontras, Glow.
- Warna ray: Biru Medis, Putih Klasik, Hijau, Ungu, Amber.
- Preset Rendah / Sedang / Tinggi / Maks, efek garis pindai, simpan sebagai PNG.
- Pengaturan diingat di browser.

## Cara pakai
- **Langsung:** buka `index.html` di browser. Tombol *Buka / Ambil Foto* selalu bisa dipakai.
- **Kamera langsung di HP:** browser hanya mengizinkan kamera lewat **HTTPS**
  (atau `localhost`). Cara termudah: aktifkan GitHub Pages untuk repo ini, lalu buka
  `https://<username>.github.io/Tugas_Kampus/XRayWeb/` di HP.
- **Lokal di komputer:** `cd XRayWeb && python3 -m http.server 8000` lalu buka `http://localhost:8000`.
