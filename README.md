# Target Tabungan

Aplikasi Android **Target Tabungan** dengan tabel 3 kolom (**Tabungan**, **Nominal**, **Tanggal/Bulan/Tahun**) 61 baris, teks berjalan (*Selamat Datang di Aplikasi Hermanto*), sinkronisasi **Firebase Firestore**, dan ekspor dokumen ke **PDF**.

## Build APK Debug Otomatis (GitHub Actions & Releases)

Repositori ini telah dilengkapi alur kerja `.github/workflows/build-apk.yml` yang otomatis berjalan saat:
- **Push** ke branch `main` atau `master`
- **Push tag** versi seperti `v1.0.0`
- **Pull Request** ke branch `main` atau `master`
- **Pemicu manual** melalui tab **Actions -> Build Debug APK & Release -> Run workflow**

### Cara Mengunduh APK Langsung
1. **Melalui GitHub Releases** (unduhan langsung file `.apk`):
   - Buka menu **Releases** di halaman utama repositori GitHub Anda.
   - Pilih rilis terbaru (misalnya `debug-latest` atau tag versi `v*`) dan unduh **`Target-Tabungan-debug.apk`** pada bagian **Assets**.
2. **Melalui GitHub Actions Artifacts**:
   - Buka tab **Actions**, pilih workflow run **Build Debug APK & Release** yang berstatus hijau, lalu unduh artifact **`Target-Tabungan-debug-apk`**.
