package com.example.iknos.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.IconFactory;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.iknos.R;
import com.example.iknos.models.NoteResponse;
import com.example.iknos.network.IknosApiService;
import com.example.iknos.network.RetrofitClient;
import com.example.iknos.socket.SocketManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import android.util.Log;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.example.iknos.room.RoomDetailResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private final Map<String, Marker> userMarkers = new HashMap<>();
    private final Map<String, String> userAvatarUrlCache = new HashMap<>();
    // Cache note setiap anggota room (userId -> NoteData)
    private final Map<String, NoteResponse.NoteData> userNotesCache = new HashMap<>();
    private TextView tvActiveUsersCount;
    private MaterialSwitch switchHideLocation;
    private Button btnInstaNote;

    // Room
    private String currentRoomId;
    private String currentRoomName;

    // GPS
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isHidden = false;

    // Dialog Upload Note
    private ImageView ivSelfiePreview;
    private Bitmap capturedSelfieBitmap = null;
    private Uri currentPhotoUri = null;

    // Polling note berkala (5 detik)
    private static final long NOTE_POLL_INTERVAL_MS = 5_000L;
    private final android.os.Handler notePollingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable notePollingRunnable;




    // Membuka kamera bawaan HP
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Jika sukses, baca gambar resolusi tinggi dari currentPhotoUri
                    if (currentPhotoUri != null) {
                        try {
                            capturedSelfieBitmap = getUprightBitmap(currentPhotoUri);
                            if (ivSelfiePreview != null && capturedSelfieBitmap != null) {
                                ivSelfiePreview.setImageBitmap(capturedSelfieBitmap);
                                ivSelfiePreview.setVisibility(View.VISIBLE);
                                // Munculkan tombol hapus jika referensinya bisa didapatkan dari Parent
                                View parent = (View) ivSelfiePreview.getParent();
                                if (parent != null) {
                                    TextView tvRemove = parent.findViewById(R.id.tvRemovePhoto);
                                    if (tvRemove != null) tvRemove.setVisibility(View.VISIBLE);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal memuat foto resolusi tinggi: " + e.getMessage());
                            Toast.makeText(this, "Gagal memuat foto", Toast.LENGTH_SHORT).show();
                        }
                    } else if (result.getData() != null && result.getData().getExtras() != null) {
                        // Fallback ke thumbnail jika currentPhotoUri null
                        capturedSelfieBitmap = (Bitmap) result.getData().getExtras().get("data");
                        if (ivSelfiePreview != null && capturedSelfieBitmap != null) {
                            ivSelfiePreview.setImageBitmap(capturedSelfieBitmap);
                            ivSelfiePreview.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
    );

    /** Membaca EXIF dari foto yang diambil untuk merotasinya ke arah yang benar (upright) */
    private Bitmap getUprightBitmap(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            InputStream input = getContentResolver().openInputStream(imageUri);
            if (input == null) return bitmap;

            android.media.ExifInterface ei = new android.media.ExifInterface(input);
            int orientation = ei.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_UNDEFINED);
            input.close();

            android.graphics.Matrix matrix = new android.graphics.Matrix();
            switch (orientation) {
                case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_main);
        currentRoomId = getIntent().getStringExtra("ROOM_ID");
        currentRoomName = getIntent().getStringExtra("ROOM_NAME");

        Log.d(TAG, "Membuka Room: " + currentRoomName + " ID: " + currentRoomId);

        // MapLibre wajib di-init sebelum setContentView, jadi kita panggil di baris paling atas onCreate() — lihat catatan di bawah
        tvActiveUsersCount = findViewById(R.id.tvActiveUsersCount);
        switchHideLocation = findViewById(R.id.switchHideLocation);
        btnInstaNote = findViewById(R.id.btnInstaNote);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            mapLibreMap = map;
            mapLibreMap.setStyle("https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json", style -> {
                Log.d(TAG, "Map style berhasil dimuat");
                mapLibreMap.setCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(-6.9147, 107.6098))
                        .zoom(14)
                        .build());
            });

            // Custom InfoWindowAdapter agar Note muncul di atas marker
            mapLibreMap.setInfoWindowAdapter(new MapLibreMap.InfoWindowAdapter() {
                @androidx.annotation.Nullable
                @Override
                public View getInfoWindow(@NonNull Marker marker) {
                    String userId = marker.getTitle();
                    if (userId == null) return null;

                    View infoView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_view_note, null);
                    CircleImageView ivNoteAvatar = infoView.findViewById(R.id.ivNoteAvatar);
                    TextView tvNoteUsername = infoView.findViewById(R.id.tvNoteUsername);
                    TextView tvNoteTimestamp = infoView.findViewById(R.id.tvNoteTimestamp);
                    ImageView ivNoteImage = infoView.findViewById(R.id.ivNoteImage);
                    TextView tvNoteText = infoView.findViewById(R.id.tvNoteText);
                    View layoutNoNote = infoView.findViewById(R.id.layoutNoNote);

                    NoteResponse.NoteData note = userNotesCache.get(userId);
                    String avatarUrl = userAvatarUrlCache.get(userId);

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(MainActivity.this).load(avatarUrl).placeholder(R.mipmap.ic_launcher_round).into(ivNoteAvatar);
                    }

                    if (note != null) {
                        String username = (note.getUser() != null && note.getUser().getUsername() != null)
                                ? note.getUser().getUsername() : userId;
                        tvNoteUsername.setText(username);

                        if (note.getUpdatedAt() != null && !note.getUpdatedAt().isEmpty()) {
                            String dateStr = note.getUpdatedAt().length() >= 10
                                    ? note.getUpdatedAt().substring(0, 10) : note.getUpdatedAt();
                            tvNoteTimestamp.setText(dateStr);
                        }

                        boolean hasImage = note.getImageUrl() != null && !note.getImageUrl().isEmpty();
                        boolean hasText = note.getText() != null && !note.getText().isEmpty();

                        if (hasImage) {
                            ivNoteImage.setVisibility(View.VISIBLE);
                            Glide.with(MainActivity.this)
                                    .load(note.getImageUrl())
                                    .into(ivNoteImage);
                        } else {
                            ivNoteImage.setVisibility(View.GONE);
                        }
                        if (hasText) {
                            tvNoteText.setVisibility(View.VISIBLE);
                            tvNoteText.setText(note.getText());
                        } else {
                            tvNoteText.setVisibility(View.GONE);
                        }
                        if (!hasImage && !hasText) {
                            layoutNoNote.setVisibility(View.VISIBLE);
                        } else {
                            layoutNoNote.setVisibility(View.GONE);
                        }
                    } else {
                        tvNoteUsername.setText(userId);
                        layoutNoNote.setVisibility(View.VISIBLE);
                        ivNoteImage.setVisibility(View.GONE);
                        tvNoteText.setVisibility(View.GONE);
                    }

                    return infoView;
                }
            });

            // Klik marker -> tampilkan InfoWindow bawaan
            mapLibreMap.setOnMarkerClickListener(marker -> {
                return false; // false = biarkan MapLibre menampilkan InfoWindow
            });
        });

        if (currentRoomId != null) {
            android.content.SharedPreferences pref = getSharedPreferences("IknosPref", MODE_PRIVATE);
            String token = pref.getString("JWT_TOKEN", "");

            SocketManager.getInstance().connectSocket(token);

            fetchRoomMembersAvatar();
            fetchRoomNotes(); // ambil note semua anggota di awal
            startNotePolling(); // mulai polling berkala setiap 5 detik
            setupSocketListener();
            SocketManager.getInstance().joinRoom(currentRoomId, snapshot -> {
                for (int i = 0; i < snapshot.length(); i++) {
                    try {
                        JSONObject member = snapshot.getJSONObject(i);
                        String userId = member.getString("userId");
                        if (member.has("lat") && !member.isNull("lat")) {
                            double lat = member.getDouble("lat");
                            double lng = member.getDouble("lng");
                            runOnUiThread(() -> updateUserMarker(userId, lat, lng));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // Switch Hide Location Listener
        switchHideLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {

            isHidden = isChecked;

            io.socket.client.Socket socket = SocketManager.getInstance().getSocket();

            if (socket != null && currentRoomId != null) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("roomId", currentRoomId);
                    data.put("isHidden", isChecked); // Kirim status hide ke backend
                    socket.emit("toggle_hide", data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // Ubah tampilan marker sendiri: grayscale saat hide, berwarna saat aktif
            applyHideStateToMyMarker(isChecked);

            if (isChecked) {
                Toast.makeText(this,
                        "Lokasi disembunyikan",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Lokasi dibagikan kembali",
                        Toast.LENGTH_SHORT).show();
            }

        });



        // Tombol Insta Note dengan Popup Dialog
        btnInstaNote.setOnClickListener(v -> showInstaNoteDialog());
        checkLocationPermissions();

        // Back Press handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setEnabled(false);
                MainActivity.super.getOnBackPressedDispatcher().onBackPressed();
            }
        });


    }


    private void setupSocketListener() {
        io.socket.client.Socket socket = SocketManager.getInstance().getSocket();

        if (socket != null) {
            // Event posisi real-time dari user lain
            socket.on("location_broadcast", args -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    String userId = data.getString("userId");
                    double lat = data.getDouble("lat");
                    double lng = data.getDouble("lng");

                    Log.d(TAG, "Menerima lokasi member " + userId + ": Lat=" + lat + ", Lng=" + lng);

                    // Update UI (marker di peta) wajib di UI Thread
                    runOnUiThread(() -> updateUserMarker(userId, lat, lng));

                } catch (JSONException e) {
                    Log.e(TAG, "Gagal memparsing data broadcast: " + e.getMessage());
                }
            });

            // Event hide/unhide dari user lain -> ubah tampilan marker mereka ke grayscale / berwarna
            socket.on("user_visibility_changed", args -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    String userId = data.getString("userId");
                    boolean hidden = data.getBoolean("isHidden");
                    Log.d(TAG, "User " + userId + " hide status: " + hidden);
                    runOnUiThread(() -> applyHideStateToMarker(userId, hidden));
                } catch (JSONException e) {
                    Log.e(TAG, "Gagal parse user_visibility_changed: " + e.getMessage());
                }
            });
        }
    }

    private final Map<String, Icon> userIconCache = new HashMap<>();
    // Cache bitmap asli (berwarna) per user, untuk membuat ulang icon grayscale/berwarna
    private final Map<String, Bitmap> userBitmapCache = new HashMap<>();

    private void updateUserMarker(String userId, double lat, double lng) {
        if (mapLibreMap == null) return;

        Marker existingMarker = userMarkers.get(userId);

        if (existingMarker != null) {
            // Marker sudah ada -> cukup geser posisinya, tidak perlu render ulang foto
            existingMarker.setPosition(new LatLng(lat, lng));
        } else {
            // Marker belum ada -> perlu ambil foto profil dulu, baru buat marker
            loadAvatarAndCreateMarker(userId, lat, lng);
        }
    }

    /** Buat icon grayscale (hitam-putih) dari bitmap berwarna */
    private Icon toGrayscaleIcon(Bitmap colorBitmap) {
        Bitmap gsBitmap = Bitmap.createBitmap(colorBitmap.getWidth(), colorBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(gsBitmap);
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        cm.setSaturation(0f);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
        canvas.drawBitmap(colorBitmap, 0, 0, paint);
        return IconFactory.getInstance(this).fromBitmap(gsBitmap);
    }

    /** Terapkan tampilan grayscale / berwarna ke marker user manapun berdasarkan status hide */
    private void applyHideStateToMarker(String userId, boolean hidden) {
        Marker marker = userMarkers.get(userId);
        Bitmap bitmap = userBitmapCache.get(userId);
        if (marker == null || bitmap == null) return;

        if (hidden) {
            marker.setIcon(toGrayscaleIcon(bitmap));
        } else {
            Icon colorIcon = IconFactory.getInstance(this).fromBitmap(bitmap);
            userIconCache.put(userId, colorIcon);
            marker.setIcon(colorIcon);
        }
    }

    /** Shortcut untuk mengubah marker milik sendiri */
    private void applyHideStateToMyMarker(boolean hidden) {
        String myUserId = getMyUserId();
        if (myUserId == null) return;
        applyHideStateToMarker(myUserId, hidden);
    }

    private void fetchRoomMembersAvatar() {
        if (currentRoomId == null) return;

        RetrofitClient.getClient(this)
                .create(IknosApiService.class)
                .getRoomDetail(currentRoomId)
                .enqueue(new Callback<RoomDetailResponse>() {
                    @Override
                    public void onResponse(Call<RoomDetailResponse> call, Response<RoomDetailResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                            for (RoomDetailResponse.RoomMember member : response.body().data.members) {
                                if (member.user != null) {
                                    userAvatarUrlCache.put(member.userId, member.user.avatarUrl);
                                }
                            }
                            Log.d(TAG, "Berhasil fetch avatar " + userAvatarUrlCache.size() + " member Room");
                        }
                    }

                    @Override
                    public void onFailure(Call<RoomDetailResponse> call, Throwable t) {
                        Log.e(TAG, "Gagal fetch detail Room: " + t.getMessage());
                    }
                });
    }

    private String getAvatarUrlForUser(String userId) {
        String avatarUrl = userAvatarUrlCache.get(userId);
        return avatarUrl != null ? avatarUrl : ""; // Isi string pake icon default
    }

    private String getMyUserId() {
        android.content.SharedPreferences pref = getSharedPreferences("IknosPref", MODE_PRIVATE);
        return pref.getString("USER_ID", null); // sesuaikan key ini dengan yang kamu pakai saat simpan userId setelah login
    }

    private void loadAvatarAndCreateMarker(String userId, double lat, double lng) {
        // Cek cache dulu, supaya tidak download ulang kalau icon sudah pernah dibuat sebelumnya
        if (userIconCache.containsKey(userId)) {
            placeMarkerWithIcon(userId, lat, lng, userIconCache.get(userId));
            return;
        }

        String avatarUrl = getAvatarUrlForUser(userId);

        View markerView = LayoutInflater.from(this).inflate(R.layout.marker_user, null);
        de.hdodenhof.circleimageview.CircleImageView ivAvatar = markerView.findViewById(R.id.ivMarkerAvatar);

        Glide.with(this)
                .asBitmap()
                .load(avatarUrl)
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        ivAvatar.setImageBitmap(resource);

                        Bitmap markerBitmap = viewToBitmap(markerView);
                        // Simpan bitmap berwarna asli ke cache
                        userBitmapCache.put(userId, markerBitmap);

                        Icon icon = IconFactory.getInstance(MainActivity.this).fromBitmap(markerBitmap);
                        userIconCache.put(userId, icon);
                        placeMarkerWithIcon(userId, lat, lng, icon);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                        // Tidak perlu aksi khusus di sini
                    }

                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        // Gagal load foto -> tetap buat marker dengan icon default MapLibre
                        Log.w(TAG, "Gagal load avatar untuk user " + userId + ", pakai marker default");
                        placeMarkerWithDefaultIcon(userId, lat, lng);
                    }
                });
    }

    private void placeMarkerWithIcon(String userId, double lat, double lng, Icon icon) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(lat, lng))
                .icon(icon)
                .title(userId);

        Marker newMarker = mapLibreMap.addMarker(markerOptions);
        userMarkers.put(userId, newMarker);
    }

    private void placeMarkerWithDefaultIcon(String userId, double lat, double lng) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(userId);

        Marker newMarker = mapLibreMap.addMarker(markerOptions);
        userMarkers.put(userId, newMarker);
    }

    private Bitmap viewToBitmap(View view) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(
                view.getMeasuredWidth(),
                view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    // Fungsi untuk memunculkan Popup Upload Insta Note milik sendiri
    private void showInstaNoteDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_insta_note, null);
        TextInputEditText etNoteText = dialogView.findViewById(R.id.etNoteText);
        Button btnCaptureSelfie = dialogView.findViewById(R.id.btnCaptureSelfie);
        ivSelfiePreview = dialogView.findViewById(R.id.ivSelfiePreview);
        TextView tvRemovePhoto = dialogView.findViewById(R.id.tvRemovePhoto);

        // Jika sebelumnya sudah pernah foto, tampilkan kembali preview-nya di popup
        if (capturedSelfieBitmap != null) {
            ivSelfiePreview.setImageBitmap(capturedSelfieBitmap);
            ivSelfiePreview.setVisibility(View.VISIBLE);
            tvRemovePhoto.setVisibility(View.VISIBLE);
        }

        // Tombol hapus foto
        tvRemovePhoto.setOnClickListener(v -> {
            capturedSelfieBitmap = null;
            ivSelfiePreview.setVisibility(View.GONE);
            tvRemovePhoto.setVisibility(View.GONE);
        });

        // Tombol kamera
        btnCaptureSelfie.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                try {
                    File photoFile = File.createTempFile("photo_", ".jpg", getCacheDir());
                    currentPhotoUri = FileProvider.getUriForFile(
                            this,
                            getApplicationContext().getPackageName() + ".fileprovider",
                            photoFile
                    );
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                    cameraLauncher.launch(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal membuat file untuk foto", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Tidak ada aplikasi kamera", Toast.LENGTH_SHORT).show();
            }
        });

        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String statusText = etNoteText.getText() != null ? etNoteText.getText().toString().trim() : "";

                    if (statusText.isEmpty() && capturedSelfieBitmap == null) {
                        Toast.makeText(this, "Note tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    uploadNote(statusText, capturedSelfieBitmap);
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /** Upload note (teks dan/atau gambar) ke backend, lalu refresh cache */
    private void uploadNote(String text, Bitmap imageBitmap) {
        if (currentRoomId == null) return;
        IknosApiService api = RetrofitClient.getClient(this).create(IknosApiService.class);

        boolean hasText = !text.isEmpty();
        boolean hasImage = imageBitmap != null;

        Callback<com.example.iknos.models.BaseResponse> callback = new Callback<com.example.iknos.models.BaseResponse>() {
            @Override
            public void onResponse(Call<com.example.iknos.models.BaseResponse> call, Response<com.example.iknos.models.BaseResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Insta Note berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                    fetchRoomNotes(); // refresh cache note
                } else {
                    Toast.makeText(MainActivity.this, "Gagal update note: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<com.example.iknos.models.BaseResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (hasImage && hasText) {
            // Ubah Bitmap menjadi File sementara
            File imageFile = bitmapToTempFile(imageBitmap);
            if (imageFile == null) {
                Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
                return;
            }
            RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
            MultipartBody.Part imgPart = MultipartBody.Part.createFormData("image", imageFile.getName(), reqFile);
            RequestBody textPart = RequestBody.create(MediaType.parse("text/plain"), text);
            api.upsertNote(currentRoomId, imgPart, textPart).enqueue(callback);

        } else if (hasImage) {
            File imageFile = bitmapToTempFile(imageBitmap);
            if (imageFile == null) { Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show(); return; }
            RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
            MultipartBody.Part imgPart = MultipartBody.Part.createFormData("image", imageFile.getName(), reqFile);
            api.upsertNoteImageOnly(currentRoomId, imgPart).enqueue(callback);

        } else {
            RequestBody textPart = RequestBody.create(MediaType.parse("text/plain"), text);
            api.upsertNoteTextOnly(currentRoomId, textPart).enqueue(callback);
        }
    }

    /** Ambil semua note anggota room dan simpan di cache (userNotesCache) */
    private void fetchRoomNotes() {
        if (currentRoomId == null) return;
        IknosApiService api = RetrofitClient.getClient(this).create(IknosApiService.class);
        api.getRoomNotes(currentRoomId).enqueue(new Callback<NoteResponse>() {
            @Override
            public void onResponse(Call<NoteResponse> call, Response<NoteResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    userNotesCache.clear();
                    for (NoteResponse.NoteData note : response.body().getData()) {
                        userNotesCache.put(note.getUserId(), note);
                    }
                    Log.d(TAG, "Note cache diperbarui: " + userNotesCache.size() + " note");
                }
            }
            @Override
            public void onFailure(Call<NoteResponse> call, Throwable t) {
                Log.e(TAG, "Gagal fetch notes: " + t.getMessage());
            }
        });
    }

    /** Konversi Bitmap menjadi File sementara di cache directory */
    private File bitmapToTempFile(Bitmap bitmap) {
        try {
            File tempFile = File.createTempFile("note_selfie_", ".jpg", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);

        } else {
            startLocationUpdates();
        }
    }
    private void startLocationUpdates() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        10000)
                        .setMinUpdateIntervalMillis(10000)
                        .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    Log.d(TAG, "Lat = " + lat + " Lng = " + lng);

                    if (currentRoomId != null) {
                        String myUserId = getMyUserId();

                        if (!isHidden) {
                            // Hanya kirim ke server dan update posisi marker saat TIDAK hidden
                            SocketManager.getInstance().pushLocation(currentRoomId, lat, lng);
                            if (myUserId != null) {
                                runOnUiThread(() -> updateUserMarker(myUserId, lat, lng));
                            }
                        } else {
                            // Saat hidden: tetap tampilkan marker di posisi terakhir (tidak digeser)
                            // Pastikan marker sudah ada di peta (jika belum, buat dulu)
                            if (myUserId != null && !userMarkers.containsKey(myUserId)) {
                                runOnUiThread(() -> updateUserMarker(myUserId, lat, lng));
                            }
                        }
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        }
    }
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startLocationUpdates();

            } else {

                Toast.makeText(
                        this,
                        "Aplikasi membutuhkan izin lokasi",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();

        // 1. Matikan tracker GPS agar hemat baterai
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Hentikan polling note
        stopNotePolling();

        io.socket.client.Socket socket = SocketManager.getInstance().getSocket();
        if (socket != null) {
            // 2. Matikan pendengar event agar tidak terjadi memory leak
            socket.off("location_broadcast");

            // 3. TAMBAHKAN INI: Beri tahu backend kalau kita keluar dari Room peta
            if (currentRoomId != null) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("roomId", currentRoomId);
                    socket.emit("leave_room", data);
                    Log.d(TAG, "Memicu leave_room untuk Room: " + currentRoomId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
//    Lifecycle untuk MapView
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        // Lanjutkan polling saat Activity kembali ke foreground
        if (currentRoomId != null) startNotePolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        // Hentikan polling saat Activity tidak di foreground agar hemat baterai
        stopNotePolling();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
    /** Mulai polling note secara berkala setiap NOTE_POLL_INTERVAL_MS */
    private void startNotePolling() {
        // Hindari duplikat runnable
        notePollingHandler.removeCallbacks(notePollingRunnable);
        notePollingRunnable = new Runnable() {
            @Override
            public void run() {
                fetchRoomNotes();
                // Jadwalkan ulang setelah interval selesai
                notePollingHandler.postDelayed(this, NOTE_POLL_INTERVAL_MS);
            }
        };
        notePollingHandler.postDelayed(notePollingRunnable, NOTE_POLL_INTERVAL_MS);
        Log.d(TAG, "Note polling dimulai (interval " + NOTE_POLL_INTERVAL_MS + "ms)");
    }

    /** Hentikan polling note */
    private void stopNotePolling() {
        notePollingHandler.removeCallbacks(notePollingRunnable);
        Log.d(TAG, "Note polling dihentikan");
    }
}
