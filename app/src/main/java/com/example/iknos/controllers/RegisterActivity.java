package com.example.iknos.controllers;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.iknos.R;
import com.example.iknos.network.IknosApiService;
import com.example.iknos.models.LoginResponse;
import com.example.iknos.models.RegisterRequest;
import com.example.iknos.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etRegUsername, etRegEmail, etRegPassword;
    private Button btnRegister;
    private TextView tvToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etRegUsername = findViewById(R.id.etRegUsername);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvToLogin = findViewById(R.id.tvToLogin);

        // Aksi tombol daftar
        btnRegister.setOnClickListener(v -> {

            String usernameInput = etRegUsername.getText().toString().trim();
            String emailInput = etRegEmail.getText().toString().trim();
            String passwordInput = etRegPassword.getText().toString().trim();

            if (usernameInput.isEmpty() || emailInput.isEmpty() || passwordInput.isEmpty()) {
                // TODO: HAPUS/GANTI TOAST
                Toast.makeText(RegisterActivity.this, "Semua kolom harus diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Jalankan Retrofit
            IknosApiService apiService = RetrofitClient.getClient(RegisterActivity.this).create(IknosApiService.class);

            RegisterRequest request = new RegisterRequest(usernameInput, emailInput, passwordInput);

            apiService.register(request).enqueue(new Callback<LoginResponse>() {

                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        // TODO: HAPUS/GANTI TOAST
                        Toast.makeText(RegisterActivity.this, "Registrasi Berhasil! Silakan Login.", Toast.LENGTH_SHORT).show();
                        finish();

                    } else {
                        // TODO: HAPUS/GANTI TOAST
                        Toast.makeText(RegisterActivity.this, "Registrasi Gagal! Email/Username mungkin sudah terpakai.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    // TODO: HAPUS/GANTI TOAST
                    Toast.makeText(RegisterActivity.this, "Error Koneksi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Kembali ke halaman Login
        tvToLogin.setOnClickListener(v -> finish());
    }
}
