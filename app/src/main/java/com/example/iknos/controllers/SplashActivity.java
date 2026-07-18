package com.example.iknos.controllers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.example.iknos.R;
import com.example.iknos.socket.SocketManager;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay selama 2 detik sebelum routing
        new Handler().postDelayed(() -> {
            SharedPreferences pref = getSharedPreferences("IknosPref", MODE_PRIVATE);
            boolean isNewUser = pref.getBoolean("IS_NEW_USER", true);

            if (isNewUser) {
                // Pengguna baru: tampilkan Tutorial/Onboarding
                startActivity(new Intent(SplashActivity.this, TutorialActivity.class));
            } else {
                // Pengguna lama: cek apakah sudah pernah login (ada JWT tersimpan)
                String savedToken = pref.getString("JWT_TOKEN", null);
                if (savedToken != null && !savedToken.isEmpty()) {
                    // Token ada -> langsung masuk ke RoomActivity tanpa login ulang
                    SocketManager.getInstance().connectSocket(savedToken);
                    startActivity(new Intent(SplashActivity.this, RoomActivity.class));
                } else {
                    // Token tidak ada -> tampilkan halaman Login
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
            }
            finish();
        }, 2000);
    }
}
