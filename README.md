# Iknos - Frontend Android (Java)

Repositori ini berisi kode sumber *frontend* untuk aplikasi **Iknos**, sebuah aplikasi pemantauan lokasi *real-time* berbasis koordinasi sosial. Seluruh antarmuka UI telah disesuaikan dengan tema *Dark Mode* minimalis menggunakan warna aksen hijau neon `#00E676`.

## Daftar Isi MVP & Cakupan Frontend
1. **Autentikasi & Otorisasi**: Halaman Login & Register dengan integrasi REST API menggunakan Retrofit.
2. **Manajemen Room**: Menampilkan daftar ruangan dari server, fitur pembuatan ruangan baru, fitur bergabung ke ruangan via kode, memproses permintaan bergabung (Approval), dan tombol *Logout*.
3. **Peta Interaktif & WebSocket Kontrol**: Merender *native canvas* peta menggunakan `MapLibre SDK`, dilengkapi pengiriman/penerimaan koordinat GPS menggunakan *Socket.IO*, serta *bottom panel* untuk *toggle* "Hide Location".
4. **Insta Note**: Dialog kustom terintegrasi dengan kamera sistem untuk memperbarui status teks dan foto selfie di dalam room.
5. **Deteksi Aktif/Offline Otomatis**: Mendeteksi secara otomatis aktivitas member dan mengubah visibilitas marker berdasarkan aktivitas terakhir.

---

## Struktur Folder (Arsitektur Proyek)

Untuk memudahkan navigasi dan skalabilitas (*clean architecture*), *source code* Java pada `app/src/main/java/com/example/iknos/` telah dikelompokkan ke dalam beberapa *sub-package* sesuai ranah/domain fiturnya:

- **`controllers/`** : Berisi tampilan interaktif UI (*Activity*) dan *adapter* RecyclerView.
  - *File:* `MainActivity.java`, `LoginActivity.java`, `RoomActivity.java`, `RegisterActivity.java`, `RequestAdapter.java`, `MemberAdapter.java`, `SettingsActivity.java`, `TutorialActivity.java`
- **`models/`** : Berisi semua POJO (*Plain Old Java Object*), *data class*, form *request*, dan *response* dari API.
  - *File:* `RoomModel.java`, `LoginRequest.java`, `BaseResponse.java`, `RoomDetailResponse.java`, dll.
- **`network/`** : Berisi pengaturan HTTP Client dan definisi rute (*endpoint*).
  - *File:* `RetrofitClient.java` dan antarmuka `IknosApiService.java`.
- **`socket/`** : Berisi manajemen siklus hidup dan *wrapper* komunikasi WebSocket.
  - *File:* `SocketManager.java` (Menangani inisiasi koneksi, `join_room`, sinkronisasi *snapshot*, dan *broadcast* koordinat).
- **`map/`** : Berisi logika yang murni berhubungan dengan kanvas peta dan utilitas *marker*.
  - *File:* `MapManager.java`, `MarkerUser.java`, `NoteMarkerView.java`.

---

## Fitur & Implementasi Utama Saat Ini

### 1. Proses Autentikasi (`controllers/LoginActivity.java` & `controllers/RegisterActivity.java`)
Telah diintegrasikan sepenuhnya dengan backend menggunakan Retrofit. Pada saat *login* berhasil, aplikasi menyimpan `JWT_TOKEN` dan `USER_ID` di dalam `SharedPreferences` lalu menavigasikan pengguna ke daftar ruangan.

### 2. Manajemen & Batasan Room (`controllers/RoomActivity.java`)
- **Daftar Room:** Data ruangan diambil secara *live* dari API backend melalui metode `getMyRooms()`.
- **Aksi Pembuatan/Bergabung:** Pembuatan ruangan (maks 5 *room*) langsung direspons dengan kemunculan kode ruangan. Pengguna yang bergabung lewat kode akan masuk ke antrean *pending requests* untuk disetujui *owner* ruangan.
- **Daftar Permintaan Bergabung:** (Owner) Bisa menerima atau menolak melalui *popup dialog request* khusus.

### 3. Peta MapLibre & Pelacakan Real-time (`controllers/MainActivity.java`)
- **Native MapLibre:** Penggunaan `WebView` telah ditinggalkan dan sepenuhnya digantikan dengan pustaka `MapLibre` *native* untuk performa perenderan (terutama untuk merender *custom marker* wajah pengguna berbentuk bulat menggunakan *Glide*).
- **Socket.IO:** Diotomatisasi melalui `socket/SocketManager.java` untuk mengambil *snapshot* lokasi setiap anggota pada saat membuka peta dan menangkap pembaruan (`location_broadcast`) tiap kali ada pergerakan pengguna lain (disesuaikan dengan jeda limitasi server).

### 4. Fitur Toggle "Hide My Location"
Membagikan lokasi bersifat opsional. Tombol sakelar akan meng-*emit* event `toggle_hide` ke *WebSocket* yang menghentikan *broadcast* lokasi pengguna saat ini di sisi *server* tanpa mengeluarkannya dari sesi *Room* secara permanen.

### 5. Pengiriman Insta Note
Menyediakan integrasi pemanggilan *Intent* kamera bawaan Android untuk memotret *selfie* cepat. Hasil pindaian dikembalikan dalam bentuk `Bitmap` yang siap di-*update* bersama *text note* untuk ditampilkan pada penanda *marker* peta anggota *Room*. Pembaruan Insta Note disinkronisasi melalui mekanisme *polling* setiap 5 detik.

### 6. Deteksi Aktif/Offline Otomatis
Melakukan *polling* setiap 60 detik untuk mengecek status pembaruan terakhir (*last seen* / *timestamp*) setiap anggota yang ada di dalam Room. Jika pengguna terdeteksi tidak aktif selama 120 detik, marker pengguna di peta akan otomatis diubah warnanya menjadi abu-abu/hitam-putih (*grayscale*) sebagai indikator *offline*. Pengguna aktif akan terus digambarkan dengan marker berwarna.

---

## Libraries Terpasang (`build.gradle.kts`)
* `Retrofit (v2.9.0)` & `Gson`: HTTP Client dan *converter* JSON API.
* `Socket.IO Client (v2.1.0)`: Penghubung WebSocket *real-time*.
* `MapLibre Android SDK`: Mesin *render* peta vektor 3D performa tinggi.
* `Glide (v4.16.0)` & `CircleImageView (v3.1.0)`: Manipulasi dan asinkron *loading* foto profil anggota ke peta.
* `Play Services Location`: Manajemen kueri GPS akurat (`FusedLocationProviderClient`).