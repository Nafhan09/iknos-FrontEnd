package com.example.iknos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private WebView mapWebView;
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
        setContentView(R.layout.activity_main);
        currentRoomId = getIntent().getStringExtra("ROOM_ID");
        currentRoomName = getIntent().getStringExtra("ROOM_NAME");

        Log.d(TAG, "Membuka Room: " + currentRoomName + " ID: " + currentRoomId);

        mapWebView = findViewById(R.id.mapWebView);
        tvActiveUsersCount = findViewById(R.id.tvActiveUsersCount);
        switchHideLocation = findViewById(R.id.switchHideLocation);
        btnInstaNote = findViewById(R.id.btnInstaNote);

        // setup WebView
        WebSettings webSettings = mapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        mapWebView.setWebViewClient(new WebViewClient());
        mapWebView.loadUrl("https://mapcn.dev/mock-map-dark");

        // setelah WebView siap
        // Di dalam onCreate(), ganti bagian inisialisasi socket menjadi:
        if (currentRoomId != null) {
            // Ambil token dari SharedPreferences secara aman
            android.content.SharedPreferences pref = getSharedPreferences("IknosPref", MODE_PRIVATE);
            String token = pref.getString("JWT_TOKEN", "");

            // Pastikan koneksi socket utama terjaga
            SocketManager.getInstance().connectSocket(token);

            // Baru nyalakan listener dan join room
            setupSocketListener();
            SocketManager.getInstance().joinRoom(currentRoomId);
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
                if (mapWebView.canGoBack()) {
                    mapWebView.goBack();
                } else {
                    setEnabled(false);
                    MainActivity.super.getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });


    }


    private void setupSocketListener() {
        // Ambil instance socket yang sedang aktif
        io.socket.client.Socket socket = SocketManager.getInstance().getSocket();


        if (socket != null) {
            // Mendengarkan broadcast lokasi dari member lain di room yang sama
            socket.on("location_broadcast", args -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    // Ambil data dari server sesuai format BE
                    String userId = data.getString("userId");
                    double lat = data.getDouble("lat");
                    double lng = data.getDouble("lng");

                    Log.d(TAG, "Menerima lokasi member " + userId + ": Lat=" + lat + ", Lng=" + lng);

                    // PENTING: Karena Socket berjalan di background thread,
                    // kita wajib menggunakan runOnUiThread untuk menyuapi data ke WebView (UI Thread)
                    runOnUiThread(() -> {
                        // Sesuai kesepakatan fungsi JavaScript di template WebView FE
                        // Misal fungsi di JS-nya bernama window.updateMemberMarker(userId, lat, lng)
                        String jsCommand = String.format("if(window.updateMemberMarker){ window.updateMemberMarker('%s', %f, %f); }", userId, lat, lng);

                        // Eksekusi fungsi JavaScript tersebut di dalam WebView
                        mapWebView.evaluateJavascript(jsCommand, null);
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "Gagal memparsing data broadcast: " + e.getMessage());
                }
            });
        }
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
                        SocketManager.getInstance()
                                .pushLocation(currentRoomId, lat, lng);
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
}