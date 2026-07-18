package com.example.iknos.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;

import com.example.iknos.R;
import com.example.iknos.network.IknosApiService;
import com.example.iknos.models.LoginRequest;
import com.example.iknos.models.LoginResponse;

import com.example.iknos.network.RetrofitClient;
import com.example.iknos.socket.SocketManager;
import com.google.android.material.textfield.TextInputEditText;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvToRegister = findViewById(R.id.tvToRegister);

        // Aksi tombol login
        btnLogin.setOnClickListener(v -> {

            String emailInput = etEmail.getText().toString().trim();
            String passwordInput = etPassword.getText().toString().trim();

            if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                // TODO: HAPUS/GANTI TOAST
                Toast.makeText(LoginActivity.this, "Semua kolom harus diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Jalankan Retrofit
            IknosApiService apiService = RetrofitClient.getClient(LoginActivity.this).create(IknosApiService.class);
            LoginRequest request = new LoginRequest(emailInput, passwordInput);

            apiService.login(request).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                        // Ambil token JWT dan id user dari backend
                        String jwtToken = response.body().getData().getToken();
                        String userId = response.body().getData().getUser().getId();

                        // Hubungkan ke Socket.IO server
                        // Parameter jwtToken berfungsi untuk validasi user
                        SocketManager.getInstance().connectSocket(jwtToken);

                        // Simpan jwtToken dan userId ke penyimpanan lokal aplikasi dengan menggunakan SharedPreferences
                        // Tandai pengguna sebagai 'lama' (IS_NEW_USER = false) agar splash tidak muncul lagi
                        SharedPreferences pref = getSharedPreferences("IknosPref", MODE_PRIVATE);
                        pref.edit()
                                .putString("JWT_TOKEN", jwtToken)
                                .putString("USER_ID", userId)
                                .putBoolean("IS_NEW_USER", false)
                                .apply();

                        // TODO: HAPUS/GANTI TOAST
                        Toast.makeText(LoginActivity.this, "Login Berhasil!", Toast.LENGTH_SHORT).show();

                        // Intent untuk Pindah ke RoomActivity
                        Intent intent = new Intent(LoginActivity.this, RoomActivity.class);
                        startActivity(intent);
                        finish();

                    } else {
                        // TODO: HAPUS/GANTI TOAST
                        Toast.makeText(LoginActivity.this, "Login Gagal! Cek email/password.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    // TODO: HAPUS/GANTI TOAST
                    Toast.makeText(LoginActivity.this, "Error Koneksi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Intent untuk Pindah ke halaman Register
        tvToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}