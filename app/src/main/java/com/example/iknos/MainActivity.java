package com.example.iknos;

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
import org.maplibre.android.maps.Style;

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
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import android.util.Log;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import androidx.annotation.NonNull;
import com.example.iknos.room.RoomDetailResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private final Map<String, Marker> userMarkers = new HashMap<>();
    private final Map<String, String> userAvatarUrlCache = new HashMap<>();
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

    // Popup
    private ImageView ivSelfiePreview;
    private Bitmap capturedSelfieBitmap = null;




    // Membuka kamera bawaan HP
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        capturedSelfieBitmap = (Bitmap) extras.get("data");
                        if (ivSelfiePreview != null && capturedSelfieBitmap != null) {
                            ivSelfiePreview.setImageBitmap(capturedSelfieBitmap);
                            ivSelfiePreview.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
    );

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
                // Style peta sudah siap dipakai (misal untuk nambah marker nanti)
                Log.d(TAG, "Map style berhasil dimuat");

                // Set posisi awal kamera (contoh: Bandung, sesuaikan default sesuai kebutuhan)
                mapLibreMap.setCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(-6.9147, 107.6098))
                        .zoom(14)
                        .build());
            });
        });

        if (currentRoomId != null) {
            android.content.SharedPreferences pref = getSharedPreferences("IknosPref", MODE_PRIVATE);
            String token = pref.getString("JWT_TOKEN", "");

            SocketManager.getInstance().connectSocket(token);

            fetchRoomMembersAvatar();
            setupSocketListener();
            SocketManager.getInstance().joinRoom(currentRoomId, snapshot -> {
                for (int i = 0; i < snapshot.length(); i++) {
                    try {
                        JSONObject member = snapshot.getJSONObject(i);
                        String userId = member.getString("userId");
                        // Asumsikan snapshot mengembalikan lat dan lng (bisa null jika belum ada update)
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

                    socket.emit("toggle_hide", data);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

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
        }
    }

    private final Map<String, Icon> userIconCache = new HashMap<>();

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

        // TODO: ganti dengan avatarUrl asli dari data member Room (lihat catatan di bawah)
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

    // Fungsi untuk memunculkan Popup Insta Note
    private void showInstaNoteDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_insta_note, null);
        TextInputEditText etNoteText = dialogView.findViewById(R.id.etNoteText);
        Button btnCaptureSelfie = dialogView.findViewById(R.id.btnCaptureSelfie);
        ivSelfiePreview = dialogView.findViewById(R.id.ivSelfiePreview);

        // Jika sebelumnya sudah pernah foto, tampilkan kembali preview-nya di popup
        if (capturedSelfieBitmap != null) {
            ivSelfiePreview.setImageBitmap(capturedSelfieBitmap);
            ivSelfiePreview.setVisibility(View.VISIBLE);
        }

        // Aksi tombol kamera di dalam popup
        btnCaptureSelfie.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

        // Buat Frame Popup dengan Material Design Dark
        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String statusText = etNoteText.getText() != null ? etNoteText.getText().toString().trim() : "";

                    // Logika validasi MVP Poin 8 & 9
                    if (statusText.isEmpty() && capturedSelfieBitmap == null) {
                        Toast.makeText(this, "Gagal: Note tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Note otomatis ter-update (menimpa note lama)
                        Toast.makeText(this, "Insta Note Berhasil Diperbarui!", Toast.LENGTH_SHORT).show();
                        // TODO: Bagian teman backend untuk mem-push data teks/foto status ini ke server
                    }
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
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
                if (isHidden) return;

                for (Location location : locationResult.getLocations()) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    Log.d(TAG, "Lat = " + lat + " Lng = " + lng);

                    if (currentRoomId != null) {
                        SocketManager.getInstance().pushLocation(currentRoomId, lat, lng);

                        String myUserId = getMyUserId();
                        if (myUserId != null) {
                            updateUserMarker(myUserId, lat, lng);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
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
}
