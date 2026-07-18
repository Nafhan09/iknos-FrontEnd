package com.example.iknos.controllers;

import com.example.iknos.R;
import com.example.iknos.ui.LoginActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay selama 2 detik sebelum pindah halaman
        new Handler().postDelayed(() -> {
            SharedPreferences pref = getSharedPreferences("IknosPrefs", MODE_PRIVATE);
            boolean isNewUser = pref.getBoolean("isNewUser", true);

            if (isNewUser) {
                // Jika pengguna baru, arahkan ke halaman Tutorial/Onboarding
                startActivity(new Intent(SplashActivity.this, TutorialActivity.class));
            } else {
                // Jika pengguna lama, langsung ke halaman Login
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, 2000);
    }
}
