package com.example.iknos.controllers;

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
import java.util.Map;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.iknos.R;
import com.example.iknos.models.BaseResponse;
import com.example.iknos.models.NoteResponse;
import com.example.iknos.network.IknosApiService;
import com.example.iknos.network.RetrofitClient;
import com.example.iknos.socket.SocketManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.example.iknos.models.RoomDetailResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private final Map<String, Marker> userMarkers = new HashMap<>();
    private final Map<String, String> userAvatarUrlCache = new HashMap<>();
    private final Map<String, String> userUsernameCache = new HashMap<>();
    // Cache note setiap anggota room (userId -> NoteData)
    private final Map<String, NoteResponse.NoteData> userNotesCache = new HashMap<>();
    
    // Status offline/online
    private int totalRoomMembers = 0;
    private final Map<String, Long> userLastSeenMap = new HashMap<>();
    private final Map<String, Boolean> userHiddenMap = new HashMap<>();
    private TextView tvActiveUsersCount;
    private MaterialSwitch switchHideLocation;
    private Button btnInstaNote;
    private android.widget.ImageButton btnShowMembers;

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

    // Polling status aktif berkala (60 detik)
    private static final long ACTIVE_POLL_INTERVAL_MS = 60_000L;
    private final android.os.Handler activeStatusHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable activeStatusRunnable;




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
                            Snackbar.make(findViewById(android.R.id.content), "Gagal memuat foto", Snackbar.LENGTH_SHORT).show();
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

        // Tombol Leave Room
        Button btnLeaveRoom = findViewById(R.id.btnLeaveRoom);
        btnLeaveRoom.setOnClickListener(v -> showLeaveRoomConfirmation());

        // Tombol Tampilkan Member
        btnShowMembers = findViewById(R.id.btnShowMembers);
        btnShowMembers.setOnClickListener(v -> showMembersDialog());

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
                    TextView tvNoteCoordinates = infoView.findViewById(R.id.tvNoteCoordinates);
                    TextView tvNoteTimeAgo = infoView.findViewById(R.id.tvNoteTimeAgo);
                    View layoutNoNote = infoView.findViewById(R.id.layoutNoNote);

                    NoteResponse.NoteData note = userNotesCache.get(userId);
                    String avatarUrl = userAvatarUrlCache.get(userId);

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(MainActivity.this).load(avatarUrl).placeholder(R.mipmap.ic_launcher_round).into(ivNoteAvatar);
                    }

                    Button btnDeleteNote = infoView.findViewById(R.id.btnDeleteNote);

                    LatLng pos = marker.getPosition();
                    if (pos != null) {
                        tvNoteCoordinates.setText(String.format(java.util.Locale.getDefault(), "Koordinat: %.4f, %.4f", pos.getLatitude(), pos.getLongitude()));
                        tvNoteCoordinates.setVisibility(View.VISIBLE);
                    } else {
                        tvNoteCoordinates.setVisibility(View.GONE);
                    }

                    if (note != null) {
                        String username = (note.getUser() != null && note.getUser().getUsername() != null)
                                ? note.getUser().getUsername() : userId;
                        tvNoteUsername.setText(username);

                        if (note.getUpdatedAt() != null && !note.getUpdatedAt().isEmpty()) {
                            String dateStr = note.getUpdatedAt().length() >= 10
                                    ? note.getUpdatedAt().substring(0, 10) : note.getUpdatedAt();
                            tvNoteTimestamp.setText(dateStr);

                            try {
                                String dateString = note.getUpdatedAt();
                                java.text.SimpleDateFormat sdf;
                                if (dateString.contains(".")) {
                                    sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                                } else {
                                    sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault());
                                }
                                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                java.util.Date date = sdf.parse(dateString);
                                if (date != null) {
                                    CharSequence timeAgo = android.text.format.DateUtils.getRelativeTimeSpanString(
                                            date.getTime(), System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS);
                                    tvNoteTimeAgo.setText(timeAgo);
                                    tvNoteTimeAgo.setVisibility(View.VISIBLE);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                tvNoteTimeAgo.setVisibility(View.GONE);
                            }
                        } else {
                            tvNoteTimeAgo.setVisibility(View.GONE);
                        }

                        if (note.getImageUrl() != null && !note.getImageUrl().isEmpty()) {
                            ivNoteImage.setVisibility(View.VISIBLE);
                            Glide.with(MainActivity.this).load(note.getImageUrl()).into(ivNoteImage);
                        } else {
                            ivNoteImage.setVisibility(View.GONE);
                        }

                        if (note.getText() != null && !note.getText().isEmpty()) {
                            tvNoteText.setVisibility(View.VISIBLE);
                            tvNoteText.setText(note.getText());
                        } else {
                            tvNoteText.setVisibility(View.GONE);
                        }

                        layoutNoNote.setVisibility(View.GONE);
                        // Hanya owner note yang bisa hapus
                        String myUserId = getSharedPreferences("IknosPref", MODE_PRIVATE).getString("USER_ID", "");
                        if (myUserId.equals(userId)) {
                            btnDeleteNote.setVisibility(View.VISIBLE);
                            btnDeleteNote.setOnClickListener(v -> {
                                deleteNote();
                                marker.hideInfoWindow();
                            });
                        } else {
                            btnDeleteNote.setVisibility(View.GONE);
                        }
                    } else {
                        tvNoteUsername.setText(userUsernameCache.containsKey(userId) ? userUsernameCache.get(userId) : userId);
                        tvNoteTimestamp.setText("");
                        ivNoteImage.setVisibility(View.GONE);
                        tvNoteText.setVisibility(View.GONE);
                        tvNoteTimeAgo.setVisibility(View.GONE);
                        layoutNoNote.setVisibility(View.VISIBLE);
                        btnDeleteNote.setVisibility(View.GONE);
                    }
                    return infoView;
                }
            });
        });

        if (currentRoomId != null) {
            android.content.SharedPreferences pref = getSharedPreferences("IknosPref", MODE_PRIVATE);
            String token = pref.getString("JWT_TOKEN", "");

            SocketManager.getInstance().connectSocket(token);

            fetchRoomMembersAvatar();
            fetchRoomNotes(); // ambil note semua anggota di awal
            startNotePolling(); // mulai polling berkala setiap 5 detik
            startActiveStatusPolling(); // mulai polling status aktif
            setupSocketListener();
            SocketManager.getInstance().joinRoom(currentRoomId, snapshot -> {
                long now = System.currentTimeMillis();
                for (int i = 0; i < snapshot.length(); i++) {
                    try {
                        JSONObject member = snapshot.getJSONObject(i);
                        String userId = member.getString("userId");
                        
                        if (member.has("updatedAt") && !member.isNull("updatedAt")) {
                            String dateString = member.getString("updatedAt");
                            long time = parseDate(dateString);
                            if (time > 0) userLastSeenMap.put(userId, time);
                        }
                        
                        if (member.has("lat") && !member.isNull("lat")) {
                            double lat = member.getDouble("lat");
                            double lng = member.getDouble("lng");
                            runOnUiThread(() -> updateUserMarker(userId, lat, lng));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(this::checkActiveUsers);
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
                Snackbar.make(findViewById(android.R.id.content),
                        "Lokasi disembunyikan",
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(android.R.id.content),
                        "Lokasi dibagikan kembali",
                        Snackbar.LENGTH_SHORT).show();
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


    // Fungsi Tampilkan Daftar Member Room
    private void showMembersDialog() {
        if (currentRoomId == null || currentRoomId.isEmpty()) return;

        IknosApiService apiService = RetrofitClient.getClient(this).create(IknosApiService.class);
        apiService.getRoomDetail(currentRoomId).enqueue(new Callback<RoomDetailResponse>() {
            @Override
            public void onResponse(Call<RoomDetailResponse> call, Response<RoomDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_member_list, null);
                    RecyclerView rvMembers = dialogView.findViewById(R.id.rvMembers);
                    rvMembers.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                    MemberAdapter adapter = new MemberAdapter(MainActivity.this, response.body().data.members);
                    rvMembers.setAdapter(adapter);

                    androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(MainActivity.this)
                            .setView(dialogView)
                            .setBackground(androidx.core.content.ContextCompat.getDrawable(MainActivity.this, R.drawable.bg_dialog_dark))
                            .setNegativeButton("Tutup", null)
                            .show();

                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#00E676"));

                    android.view.Window window = dialog.getWindow();
                    if (window != null) {
                        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                        int width = (int) (metrics.widthPixels * 0.90);
                        window.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                    }
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "Gagal mengambil data member", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RoomDetailResponse> call, Throwable t) {
                Snackbar.make(findViewById(android.R.id.content), "Error koneksi: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
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

                    userLastSeenMap.put(userId, System.currentTimeMillis());

                    // Update UI (marker di peta) wajib di UI Thread
                    runOnUiThread(() -> {
                        updateUserMarker(userId, lat, lng);
                        checkActiveUsers();
                    });

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
                    userHiddenMap.put(userId, hidden);
                    runOnUiThread(() -> updateMarkerVisualState(userId));
                } catch (JSONException e) {
                    Log.e(TAG, "Gagal parse user_visibility_changed: " + e.getMessage());
                }
            });
        }
    }

    /** Update label jumlah user aktif berdasarkan jumlah marker di peta */
    /** Update label jumlah user aktif berdasarkan timestamp last seen */
    private void checkActiveUsers() {
        long now = System.currentTimeMillis();
        int activeCount = 0;
        
        for (String userId : userMarkers.keySet()) {
            Long lastSeen = userLastSeenMap.getOrDefault(userId, 0L);
            if ((now - lastSeen) <= 120_000L) {
                activeCount++;
            }
            updateMarkerVisualState(userId);
        }
        
        if (tvActiveUsersCount != null) {
            tvActiveUsersCount.setText(activeCount + " / " + totalRoomMembers);
        }
    }
    
    private long parseDate(String dateString) {
        try {
            java.text.SimpleDateFormat sdf;
            if (dateString.contains(".")) {
                sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
            } else {
                sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault());
            }
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(dateString);
            if (date != null) return date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
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

    /** Terapkan tampilan grayscale / berwarna ke marker berdasarkan status hide & offline */
    private void updateMarkerVisualState(String userId) {
        Marker marker = userMarkers.get(userId);
        Bitmap bitmap = userBitmapCache.get(userId);
        if (marker == null || bitmap == null) return;
        
        boolean hidden = userHiddenMap.containsKey(userId) ? userHiddenMap.get(userId) : false;
        long now = System.currentTimeMillis();
        long lastSeen = userLastSeenMap.getOrDefault(userId, 0L);
        boolean isOffline = (now - lastSeen) > 120_000L;
        
        if (hidden || isOffline) {
            marker.setIcon(toGrayscaleIcon(bitmap));
        } else {
            Icon colorIcon = userIconCache.get(userId);
            if (colorIcon == null) {
                colorIcon = IconFactory.getInstance(this).fromBitmap(bitmap);
                userIconCache.put(userId, colorIcon);
            }
            marker.setIcon(colorIcon);
        }
    }

    /** Shortcut untuk mengubah marker milik sendiri */
    private void applyHideStateToMyMarker(boolean hidden) {
        String myUserId = getMyUserId();
        if (myUserId == null) return;
        userHiddenMap.put(myUserId, hidden);
        updateMarkerVisualState(myUserId);
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
                            totalRoomMembers = response.body().data.members.size();
                            for (RoomDetailResponse.RoomMember member : response.body().data.members) {
                                if (member.user != null) {
                                    userAvatarUrlCache.put(member.userId, member.user.avatarUrl);
                                    userUsernameCache.put(member.userId, member.user.username);
                                    userHiddenMap.put(member.userId, member.isHidden);
                                    
                                    if (member.updatedAt != null) {
                                        long time = parseDate(member.updatedAt);
                                        if (time > 0) userLastSeenMap.put(member.userId, time);
                                    }

                                    if (member.lastLat != null && member.lastLng != null) {
                                        final double lat = member.lastLat;
                                        final double lng = member.lastLng;
                                        runOnUiThread(() -> loadAvatarAndCreateMarker(member.userId, lat, lng));
                                    } else {
                                        runOnUiThread(() -> {
                                            Marker existing = userMarkers.remove(member.userId);
                                            if (existing != null) {
                                                mapLibreMap.removeMarker(existing);
                                            }
                                        });
                                    }
                                }
                            }
                            runOnUiThread(() -> checkActiveUsers());
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
                        updateMarkerVisualState(userId);
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
        Marker oldMarker = userMarkers.get(userId);
        if (oldMarker != null) {
            mapLibreMap.removeMarker(oldMarker);
        }

        MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(lat, lng))
                .icon(icon)
                .title(userId);

        Marker newMarker = mapLibreMap.addMarker(markerOptions);
        userMarkers.put(userId, newMarker);
    }

    private void placeMarkerWithDefaultIcon(String userId, double lat, double lng) {
        Marker oldMarker = userMarkers.get(userId);
        if (oldMarker != null) {
            mapLibreMap.removeMarker(oldMarker);
        }

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
                    Snackbar.make(findViewById(android.R.id.content), "Gagal membuat file untuk foto", Snackbar.LENGTH_SHORT).show();
                }
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Tidak ada aplikasi kamera", Snackbar.LENGTH_SHORT).show();
            }
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setView(dialogView)
                .setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_dialog_dark))
                .setPositiveButton("Update", null) // handle secara manual agar bisa validasi
                .setNegativeButton("Batal", (d, which) -> d.dismiss())
                .create();

        dialog.show();
        
        // Override aksi tombol positif setelah dialog ditampilkan untuk validasi tanpa menutup dialog otomatis
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String statusText = etNoteText.getText() != null ? etNoteText.getText().toString().trim() : "";
            if (statusText.isEmpty() && capturedSelfieBitmap == null) {
                Snackbar.make(findViewById(android.R.id.content), "Note tidak boleh kosong! Hapus note dengan tombol Hapus di bawah", Snackbar.LENGTH_SHORT).show();
                return;
            }
            uploadNote(statusText, capturedSelfieBitmap);
            dialog.dismiss();
        });
    }

    /** Menghapus note sendiri */
    private void deleteNote() {
        if (currentRoomId == null) return;
        IknosApiService api = RetrofitClient.getClient(this).create(IknosApiService.class);
        api.deleteNote(currentRoomId).enqueue(new Callback<com.example.iknos.models.BaseResponse>() {
            @Override
            public void onResponse(Call<com.example.iknos.models.BaseResponse> call, Response<com.example.iknos.models.BaseResponse> response) {
                if (response.isSuccessful()) {
                    Snackbar.make(findViewById(android.R.id.content), "Note berhasil dihapus", Snackbar.LENGTH_SHORT).show();
                    // Karena note hilang dari DB, hapus dari cache local dan tutup info window di map
                    String myUserId = getMyUserId();
                    if (myUserId != null) userNotesCache.remove(myUserId);
                    fetchRoomNotes(); 
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "Gagal hapus note", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.iknos.models.BaseResponse> call, Throwable t) {
                Snackbar.make(findViewById(android.R.id.content), "Error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
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
                    Snackbar.make(findViewById(android.R.id.content), "Insta Note berhasil diperbarui!", Snackbar.LENGTH_SHORT).show();
                    fetchRoomNotes(); // refresh cache note
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "Gagal update note: " + response.code(), Snackbar.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<com.example.iknos.models.BaseResponse> call, Throwable t) {
                Snackbar.make(findViewById(android.R.id.content), "Error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        };

        if (hasImage && hasText) {
            // Ubah Bitmap menjadi File sementara
            File imageFile = bitmapToTempFile(imageBitmap);
            if (imageFile == null) {
                Snackbar.make(findViewById(android.R.id.content), "Gagal memproses gambar", Snackbar.LENGTH_SHORT).show();
                return;
            }
            RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
            MultipartBody.Part imgPart = MultipartBody.Part.createFormData("image", imageFile.getName(), reqFile);
            RequestBody textPart = RequestBody.create(MediaType.parse("text/plain"), text);
            api.upsertNote(currentRoomId, imgPart, textPart).enqueue(callback);

        } else if (hasImage) {
            File imageFile = bitmapToTempFile(imageBitmap);
            if (imageFile == null) { Snackbar.make(findViewById(android.R.id.content), "Gagal memproses gambar", Snackbar.LENGTH_SHORT).show(); return; }
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
                                runOnUiThread(() -> {
                                    userLastSeenMap.put(myUserId, System.currentTimeMillis());
                                    updateUserMarker(myUserId, lat, lng);
                                    checkActiveUsers();
                                });
                            }
                        } else {
                            // Saat hidden: tetap tampilkan marker di posisi terakhir (tidak digeser)
                            // Pastikan marker sudah ada di peta (jika belum, buat dulu)
                            if (myUserId != null) {
                                runOnUiThread(() -> {
                                    userLastSeenMap.put(myUserId, System.currentTimeMillis());
                                    if (!userMarkers.containsKey(myUserId)) {
                                        updateUserMarker(myUserId, lat, lng);
                                    }
                                    checkActiveUsers();
                                });
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

                Snackbar.make(
                        findViewById(android.R.id.content),
                        "Aplikasi membutuhkan izin lokasi",
                        Snackbar.LENGTH_LONG
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
        stopActiveStatusPolling();

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
        if (currentRoomId != null) {
            startNotePolling();
            startActiveStatusPolling();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        // Hentikan polling saat Activity tidak di foreground agar hemat baterai
        stopNotePolling();
        stopActiveStatusPolling();
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

    private void startActiveStatusPolling() {
        activeStatusHandler.removeCallbacks(activeStatusRunnable);
        activeStatusRunnable = new Runnable() {
            @Override
            public void run() {
                checkActiveUsers();
                activeStatusHandler.postDelayed(this, ACTIVE_POLL_INTERVAL_MS);
            }
        };
        activeStatusHandler.postDelayed(activeStatusRunnable, ACTIVE_POLL_INTERVAL_MS);
    }

    private void stopActiveStatusPolling() {
        activeStatusHandler.removeCallbacks(activeStatusRunnable);
    }

    /** Hentikan polling note */
    private void stopNotePolling() {
        notePollingHandler.removeCallbacks(notePollingRunnable);
        Log.d(TAG, "Note polling dihentikan");
    }

    /** Tampilkan dialog konfirmasi sebelum leave room */
    private void showLeaveRoomConfirmation() {
        CharSequence customTitle = HtmlCompat.fromHtml("<font color='#00E676'>Keluar dari Ruangan?</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        CharSequence customMessage = HtmlCompat.fromHtml("<font color='#FFFFFF'>Kamu akan meninggalkan ruangan ini. Kamu perlu bergabung ulang lagi untuk masuk.</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);

        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle(customTitle)
                .setMessage(customMessage)
                .setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_dialog_dark))
                .setPositiveButton("Ya, Keluar", (dialog, which) -> leaveRoom())
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /** Panggil REST API leave room, lalu kembali ke RoomActivity */
    private void leaveRoom() {
        if (currentRoomId == null) return;

        // Beritahu server via socket terlebih dahulu
        io.socket.client.Socket socket = SocketManager.getInstance().getSocket();
        if (socket != null) {
            try {
                JSONObject data = new JSONObject();
                data.put("roomId", currentRoomId);
                socket.emit("leave_room", data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Panggil REST API untuk leave permanen
        IknosApiService api = RetrofitClient.getClient(this).create(IknosApiService.class);
        api.leaveRoom(currentRoomId).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                stopNotePolling();
                stopActiveStatusPolling();
                Snackbar.make(findViewById(android.R.id.content), "Berhasil keluar dari ruangan", Snackbar.LENGTH_SHORT).show();
                // Kembali ke daftar room
                Intent intent = new Intent(MainActivity.this, RoomActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                Snackbar.make(findViewById(android.R.id.content), "Gagal keluar: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
