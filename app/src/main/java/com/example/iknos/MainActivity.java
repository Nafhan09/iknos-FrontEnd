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

public class MainActivity extends AppCompatActivity {

    private WebView mapWebView;
    private TextView tvActiveUsersCount;
    private MaterialSwitch switchHideLocation;
    private Button btnInstaNote;

    // Komponen di dalam dialog popup
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

        mapWebView = findViewById(R.id.mapWebView);
        tvActiveUsersCount = findViewById(R.id.tvActiveUsersCount);
        switchHideLocation = findViewById(R.id.switchHideLocation);
        btnInstaNote = findViewById(R.id.btnInstaNote);

        // Setup WebView
        WebSettings webSettings = mapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        mapWebView.setWebViewClient(new WebViewClient());
        // TODO: ganti dengan url mapcn.dev
        mapWebView.loadUrl("https://mapcn.dev/mock-map-dark");

        // Switch Hide Location Listener
        switchHideLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Lokasi disembunyikan (Hide Mode)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Lokasi dibagikan kembali", Toast.LENGTH_SHORT).show();
            }
        });

        // Tombol Insta Note dengan Popup Dialog
        btnInstaNote.setOnClickListener(v -> showInstaNoteDialog());

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
}