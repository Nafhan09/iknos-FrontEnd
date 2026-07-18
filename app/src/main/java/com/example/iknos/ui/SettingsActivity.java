package com.example.iknos.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

        // Tombol Logout
        btnLogout.setOnClickListener(v -> forceLogout());
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
                    Toast.makeText(SettingsActivity.this, "Gagal memuat profil", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                // TODO: GANTI/HAPUS TOAST
                Toast.makeText(SettingsActivity.this, "Error koneksi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();

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
                    Toast.makeText(SettingsActivity.this, "Avatar berhasil diperbarui", Toast.LENGTH_SHORT).show();
                } else {
                    // TODO: GANTI/HAPUS TOAST
                    Toast.makeText(SettingsActivity.this, "Gagal upload avatar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                // TODO: GANTI/HAPUS TOAST
                Toast.makeText(SettingsActivity.this, "Error upload: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    // Fungsi untuk membantu gagal validasi autentikasi
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