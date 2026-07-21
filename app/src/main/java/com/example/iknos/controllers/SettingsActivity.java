package com.example.iknos.controllers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;

import com.example.iknos.models.UpdateUsernameRequest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.iknos.R;
import com.example.iknos.models.UserProfileResponse;
import com.example.iknos.network.IknosApiService;
import com.example.iknos.network.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private CircleImageView ivAvatar;
    private TextView tvUsername, tvEmail;
    private ProgressBar progressBar;
    private FloatingActionButton fabChangeAvatar;
    private MaterialButton btnLogout;
    private MaterialToolbar toolbar;
    private ImageButton btnEditUsername;

    private String token;
    private IknosApiService apiService;

    // Fungsi untuk pilih dan upload gambar
    private final ActivityResultLauncher<String> selectImageLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                // Tampilkan gambar sementara di UI
                Glide.with(this).load(uri).into(ivAvatar);
                // Upload ke server
                uploadImageToServer(uri);
            }
        });

    // Method meliputi Layouting, validasi token dan event listener
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ivAvatar = findViewById(R.id.ivAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        progressBar = findViewById(R.id.progressBar);
        fabChangeAvatar = findViewById(R.id.fabChangeAvatar);
        btnLogout = findViewById(R.id.btnLogout);
        toolbar = findViewById(R.id.toolbarSettings);

        apiService = RetrofitClient.getClient(this).create(IknosApiService.class);

        // Ambil data kiriman dari Intent untuk pre-populate UI secara instan
        String extraUsername = getIntent().getStringExtra("EXTRA_USERNAME");
        String extraEmail = getIntent().getStringExtra("EXTRA_EMAIL");
        String extraAvatarUrl = getIntent().getStringExtra("EXTRA_AVATAR_URL");

        if (extraUsername != null && !extraUsername.isEmpty()) {
            tvUsername.setText(extraUsername);
        }
        if (extraEmail != null && !extraEmail.isEmpty()) {
            tvEmail.setText(extraEmail);
        }
        if (extraAvatarUrl != null && !extraAvatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(extraAvatarUrl)
                    .placeholder(R.mipmap.ic_launcher_round)
                    .into(ivAvatar);
        }

        // Ambil token dari SharedPreferences
        SharedPreferences prefs = getSharedPreferences("IknosPref", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", null);

        if (token != null) {
            token = "Bearer " + token;
            fetchUserProfile();
        } else {
            forceLogout();
        }

        // Tombol Kembali di Toolbar
        toolbar.setNavigationOnClickListener(v -> finish());

        // Tombol Ubah Avatar
        fabChangeAvatar.setOnClickListener(v -> {
            // Validasi format gambar
            selectImageLauncher.launch("image/*");
        });

        // Tombol Edit Username
        btnEditUsername = findViewById(R.id.btnEditUsername);
        btnEditUsername.setOnClickListener(v -> showEditUsernameDialog());

        // Tombol Logout
        btnLogout.setOnClickListener(v -> forceLogout());
    }

    // Fungsi dialog untuk ubah username
    private void showEditUsernameDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_username, null);
        EditText etNewUsername = dialogView.findViewById(R.id.etNewUsername);
        etNewUsername.setText(tvUsername.getText());

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle("Ubah Username")
                .setView(dialogView)
                .setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_dialog_dark))
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", (d, w) -> d.dismiss())
                .create();

        dialog.show();

        // Handle manual agar tidak langsung tertutup saat validasi gagal
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#00E676"));
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#888888"));
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newUsername = etNewUsername.getText().toString().trim();
            if (newUsername.isEmpty()) {
                etNewUsername.setError("Username tidak boleh kosong");
                return;
            }
            if (newUsername.length() < 3) {
                etNewUsername.setError("Username minimal 3 karakter");
                return;
            }
            dialog.dismiss();
            updateUsernameToServer(newUsername);
        });
    }

    // Fungsi request update username ke backend
    private void updateUsernameToServer(String newUsername) {
        progressBar.setVisibility(android.view.View.VISIBLE);
        apiService.updateUsername(new UpdateUsernameRequest(newUsername)).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                progressBar.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    tvUsername.setText(response.body().getData().getUsername());
                    Snackbar.make(findViewById(android.R.id.content), "Username berhasil diperbarui", Snackbar.LENGTH_SHORT).show();
                } else {
                    try {
                        String errBody = response.errorBody() != null ? response.errorBody().string() : "Gagal memperbarui username";
                        Snackbar.make(findViewById(android.R.id.content), errBody, Snackbar.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Snackbar.make(findViewById(android.R.id.content), "Gagal memperbarui username", Snackbar.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                Snackbar.make(findViewById(android.R.id.content), "Error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    // Fungsi untuk request data user
    // Fungsi meliputi penggunaan endpoint,method onResponse untuk validasi pengambilan data dan onFailure untuk validasi koneksi
    private void fetchUserProfile() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    UserProfileResponse.UserData user = response.body().getData();
                    tvUsername.setText(user.getUsername());
                    tvEmail.setText(user.getEmail());

                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        Glide.with(SettingsActivity.this)
                                .load(user.getAvatarUrl())
                                .placeholder(R.mipmap.ic_launcher_round)
                                .into(ivAvatar);
                    }
                } else {
                    // TODO: GANTI/HAPUS TOAST
                    Snackbar.make(findViewById(android.R.id.content), "Gagal memuat profil", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                // TODO: GANTI/HAPUS TOAST
                Snackbar.make(findViewById(android.R.id.content), "Error koneksi: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    // Fungsi untuk proses update avatar
    // Fungsi meliputi pemanggilan fungsi getFileFromUri(), validasi file dan update ke server
    private void uploadImageToServer(Uri fileUri) {
        progressBar.setVisibility(View.VISIBLE);

        // Pemilihan file dilakukan oleh fungsi getFileFromUri()
        File file = getFileFromUri(fileUri);
        // Validasi file yang dipilih
        if (file == null) {
            // TODO: GANTI/HAPUS TOAST
            Snackbar.make(findViewById(android.R.id.content), "Gagal memproses gambar", Snackbar.LENGTH_SHORT).show();

            progressBar.setVisibility(View.GONE);
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("avatar", file.getName(), requestFile);

        // Proses upload avatar melalui endpoint @PUT("users/me/avatar")
        apiService.uploadAvatar(body).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    // TODO: GANTI/HAPUS TOAST
                    Snackbar.make(findViewById(android.R.id.content), "Avatar berhasil diperbarui", Snackbar.LENGTH_SHORT).show();
                } else {
                    // TODO: GANTI/HAPUS TOAST
                    Snackbar.make(findViewById(android.R.id.content), "Gagal upload avatar", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                // TODO: GANTI/HAPUS TOAST
                Snackbar.make(findViewById(android.R.id.content), "Error upload: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    // Fungsi helper untuk mengubah URI galeri menjadi File fisik sementara agar bisa diupload
    private File getFileFromUri(Uri uri) {
        try {
            // InputStream untuk membaca file
            InputStream inputStream = getContentResolver().openInputStream(uri);

            // Buat Temporary File
            File tempFile = File.createTempFile("avatar_", ".jpg", getCacheDir());

            // Menyalin data file ke dalam Temporary File
            OutputStream out = new FileOutputStream(tempFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            out.close();
            inputStream.close();

            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Fungsi logout
    // Fungsi meliputi pembersihan preference dan memindahkan activity pada loginActivity
    private void forceLogout() {
        SharedPreferences prefs = getSharedPreferences("IknosPref", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}