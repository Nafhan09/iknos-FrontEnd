package com.example.iknos;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;

public class MainActivity extends AppCompatActivity {

    private WebView mapWebView;
    private TextView tvActiveUsersCount;
    private MaterialSwitch switchHideLocation;
    private Button btnInstaNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi komponen ui
        mapWebView = findViewById(R.id.mapWebView);
        tvActiveUsersCount = findViewById(R.id.tvActiveUsersCount);
        switchHideLocation = findViewById(R.id.switchHideLocation);
        btnInstaNote = findViewById(R.id.btnInstaNote);

        // Tangkap data nama room dari RoomActivity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("ROOM_NAME")) {
            String roomName = intent.getStringExtra("ROOM_NAME");
            Toast.makeText(this, "Masuk ke: " + roomName, Toast.LENGTH_SHORT).show();
        }

        // Setup WebView untuk rendering mapcn.dev
        WebSettings webSettings = mapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBuiltInZoomControls(false);

        mapWebView.setWebViewClient(new WebViewClient());
        mapWebView.loadUrl("https://mapcn.dev/mock-map-dark");

        // Logika Aksi Switch "Hide My Location"
        switchHideLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Lokasi disembunyikan (Hide Mode)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Lokasi dibagikan kembali", Toast.LENGTH_SHORT).show();
            }
        });

        // Logic Tombol Insta Note
        btnInstaNote.setOnClickListener(v -> {
            Toast.makeText(this, "Membuka status Insta Note...", Toast.LENGTH_SHORT).show();
        });

        // Menggunakan OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mapWebView.canGoBack()) {
                    mapWebView.goBack();
                } else {
                    // Jika webview tidak bisa back, matikan activity dan kembali ke RoomActivity
                    setEnabled(false);
                    MainActivity.super.getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }
}