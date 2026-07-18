package com.example.iknos.controllers;

import com.example.iknos.R;
import com.example.iknos.ui.LoginActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

public class TutorialActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext;
    private final String[] titles = {"Selamat Datang di Iknos",
            "Buat & Gabung Room",
            "Bagikan Insta Note",
            "Siap Beraksi?"};
    private final String[] descs = {
            "Aplikasi koordinasi sosial real-time. Pantau posisi teman-teman terdekatmu secara langsung di atas peta.",
            "Tekan tombol (+) untuk membuat Room baru atau masukkan kode unik untuk bergabung. Sebagai pemilik Room, kamu bisa menerima atau menolak permintaan gabung dari user lain.",
            "Berikan pembaruan status instan ke seluruh anggota Room. Tulis pesan singkat atau bagikan foto selfie serumu saat beraktivitas.",
            "Tekan tombol di bawah untuk masuk ke halaman utama dan mulai pantau pergerakan timmu!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        viewPager = findViewById(R.id.viewPagerTutorial);
        btnNext = findViewById(R.id.btnNextTutorial);

        viewPager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tutorial_slide, parent, false);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TextView title = holder.itemView.findViewById(R.id.tvSlideTitle);
                TextView desc = holder.itemView.findViewById(R.id.tvSlideDesc);
                title.setText(titles[position]);
                desc.setText(descs[position]);
            }

            @Override
            public int getItemCount() {
                return titles.length;
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == titles.length - 1) {
                    btnNext.setText("MULAI APLIKASI");
                } else {
                    btnNext.setText("LANJUT");
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < titles.length - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                // Pengguna menyelesaikan tutorial, simpan status ke SharedPreferences
                SharedPreferences pref = getSharedPreferences("IknosPrefs", MODE_PRIVATE);
                pref.edit().putBoolean("isNewUser", false).apply();

                // Pindah ke halaman Login milik temanmu
                startActivity(new Intent(TutorialActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}
