# Iknos - Frontend Android (Java) 📍

Repositori ini berisi kode sumber *frontend* untuk aplikasi **Iknos**, sebuah aplikasi pemantauan lokasi *real-time* berbasis koordinasi sosial. Seluruh antarmuka UI telah disesuaikan dengan tema *Dark Mode* minimalis menggunakan warna aksen hijau neon `#00E676`.

## 📑 Daftar Isi MVP & Cakupan Frontend
1. **Autentikasi & Otorisasi**: Halaman Login & Register dengan validasi input dasar
2. **Manajemen Room**: Menampilkan daftar ruangan (maksimal 5 *room*), fitur pembuatan ruangan baru, fitur bergabung ke ruangan via kode, dan tombol *Logout*.
3. **Peta Interaktif & WebSocket Kontrol**: Menggunakan kontainer `WebView` untuk merender canvas dari `mapcn.dev`, dilengkapi *top panel* jumlah pengguna (maksimal 10 pengguna) dan *bottom panel* untuk toggle "Hide Location".
4. **Insta Note**: Dialog kustom terintegrasi dengan kamera sistem untuk melakukan pembaruan status teks, foto selfie, atau keduanya.

---

## 🛠️ Panduan Integrasi Backend & WebSocket

Untuk memudahkan proses *wiring* (penyambungan) logika backend ke komponen UI yang sudah siap pakai, berikut adalah panduan titik integrasi pada kelas-kelas Java:

### 1. Proses Autentikasi (`LoginActivity.java` & `RegisterActivity.java`)
* **Titik Integrasi Login:** Buka `LoginActivity.java`. Tambahkan HTTP Request (Retrofit/Volley) di sana. Jika sukses, arahkan ke `RoomActivity.class`.
* **Titik Integrasi Register:** Buka `RegisterActivity.java`, kirimkan data user baru.

### 2. Manajemen & Batasan Room (`RoomActivity.java`)
* **Mengisi List Room:** Saat ini `RecyclerView` menggunakan *mock data* (tiruan). Ubah *source* `mockRoomList` dengan data riil hasil *fetch* dari Firebase/PostgreSQL.
* **Buat & Gabung Ruangan:** Cari fungsi `showAddRoomDialog()`. Di dalam blok *PositiveButton*, terdapat dua percabangan `// TODO:` untuk memicu fungsi API *Create Room* atau *Join Room*.

### 3. Canvas Peta `mapcn.dev` & Koordinat (`MainActivity.java`)
* **URL Peta Kustom:** Cari baris `mapWebView.loadUrl("https://mapcn.dev/mock-map-dark");`. Ganti URL tiruan tersebut dengan URL visualisasi peta interaktif kalian yang valid.
* **Limitasi Interval (10 Detik):** Pastikan skrip WebSocket pada URL peta tersebut melakukan pembaruan data koordinat dengan interval 10 detik

### 4. Fitur Toggle "Hide My Location" (`MainActivity.java`)
* **Logika Sakelar:** Cari komponen `switchHideLocation.setOnCheckedChangeListener`. 
    * Kondisi `isChecked == true`: Kirim sinyal ke server/WebSocket untuk menghentikan siaran (*broadcast*) lokasi pengguna saat ini tanpa mengeluarkannya dari *Room*
    * Kondisi `isChecked == false`: Mulai kembali pengiriman koordinat GPS.

### 5. Pengiriman Insta Note (`MainActivity.java`)
* **Data Status:** Cari fungsi `showInstaNoteDialog()` di bagian tombol *Update*. 
    * Teks status tersimpan pada variabel `statusText`.
    * Foto selfie biner tersimpan pada variabel `capturedSelfieBitmap` (siap dikonversi ke Base64 atau Multipart Form Data).
    * *Rule:* Data note baru ini harus menimpa/menghapus note lama pengguna di ruangan tersebut.

---

## 🚀 Libraries Terpasang (`build.gradle.kts`)
Aplikasi ini sudah dilengkapi dengan beberapa dependensi penting:
* `OkHttp (v4.12.0)`: Siap digunakan untuk koneksi *WebSocket client*.
* `Glide (v4.16.0)` & `CircleImageView (v3.1.0)`: Siap digunakan untuk merender foto profil pengguna menjadi bulat secara dinamis di atas peta.

---