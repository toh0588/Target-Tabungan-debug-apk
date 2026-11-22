# Target Tabungan

Repositori: **`toh0588/Target-Tabungan-debug-apk`**

Aplikasi Android **Target Tabungan** dengan tabel 3 kolom (**Tabungan**, **Nominal**, **Tanggal/Bulan/Tahun**) 61 baris, teks berjalan (*Selamat Datang di Aplikasi Hermanto*), sinkronisasi **Firebase Firestore**, dan ekspor dokumen ke **PDF**.

## Folder Artefak Bangunan (`artifacts/`)

Semua hasil build APK Debug tersedia langsung di folder [`artifacts/`](https://github.com/toh0588/Target-Tabungan-debug-apk/tree/main/artifacts):
- [`artifacts/Target-Tabungan-debug.apk`](https://github.com/toh0588/Target-Tabungan-debug-apk/raw/main/artifacts/Target-Tabungan-debug.apk)
- [`artifacts/output-metadata.json`](https://github.com/toh0588/Target-Tabungan-debug-apk/blob/main/artifacts/output-metadata.json)
- [`artifacts/SHA256SUMS.txt`](https://github.com/toh0588/Target-Tabungan-debug-apk/blob/main/artifacts/SHA256SUMS.txt)

## Build APK Debug Otomatis (GitHub Actions & Releases)

Repositori ini telah dilengkapi alur kerja `.github/workflows/build-apk.yml` yang otomatis berjalan saat:
- **Push** ke branch `main` atau `master` (otomatis memperbarui folder `artifacts/`, mengunggah artifact Actions, dan mempublikasikan Release)
- **Push tag** versi seperti `v1.0.0`
- **Pull Request** ke branch `main` atau `master`
- **Pemicu manual** melalui tab **Actions -> Build Debug APK & Release -> Run workflow**

### Cara Mengunduh APK Langsung
1. **Melalui Folder Artefak (`artifacts/`)**:
   - Buka folder [`artifacts/`](https://github.com/toh0588/Target-Tabungan-debug-apk/tree/main/artifacts) lalu unduh **`Target-Tabungan-debug.apk`**.
2. **Melalui GitHub Releases**:
   - Buka halaman [**Releases**](https://github.com/toh0588/Target-Tabungan-debug-apk/releases), pilih rilis terbaru (`debug-latest` atau tag `v*`), lalu unduh **`Target-Tabungan-debug.apk`** pada bagian **Assets**.
3. **Melalui GitHub Actions Artifacts**:
   - Buka tab [**Actions**](https://github.com/toh0588/Target-Tabungan-debug-apk/actions), pilih workflow run **Build Debug APK & Release** yang selesai, lalu unduh artifact **`Target-Tabungan-debug-apk`**.
