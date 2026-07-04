package com.example.iknos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class RoomActivity extends AppCompatActivity {

    private RecyclerView rvRooms;
    private FloatingActionButton fabAddRoom;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        rvRooms = findViewById(R.id.rvRooms);
        fabAddRoom = findViewById(R.id.fabAddRoom);
        btnLogout = findViewById(R.id.btnLogout);

        // Setup RecyclerView
        rvRooms.setLayoutManager(new LinearLayoutManager(this));

        List<String[]> mockRoomList = new ArrayList<>();
        mockRoomList.add(new String[]{"Room Liburan Keluarga", "Code: FAM-123", "4/10"});
        mockRoomList.add(new String[]{"Tim Dev Iknos Project", "Code: IKNS-99", "2/10"});
        mockRoomList.add(new String[]{"Koordinasi CFD Jakarta", "Code: CFD-202", "8/10"});

        rvRooms.setAdapter(new RecyclerView.Adapter<RoomViewHolder>() {
            @NonNull
            @Override
            public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
                return new RoomViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
                String[] roomData = mockRoomList.get(position);
                holder.tvName.setText(roomData[0]);
                holder.tvCode.setText(roomData[1]);
                holder.tvCount.setText(roomData[2]);

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(RoomActivity.this, MainActivity.class);
                    intent.putExtra("ROOM_NAME", roomData[0]);
                    startActivity(intent);
                });
            }

            @Override
            public int getItemCount() {
                return mockRoomList.size();
            }
        });

        // Fitur Logout
        btnLogout.setOnClickListener(v -> {
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show();
            // Kembalikan pengguna ke halaman LoginActivity dan hapus tumpukan history activity
            Intent intent = new Intent(RoomActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Fitur Tambah/Gabung Room
        fabAddRoom.setOnClickListener(v -> showAddRoomDialog());
    }

    // Fungsi popup penambahan room
    private void showAddRoomDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_room, null);
        TextInputEditText etNewRoomName = dialogView.findViewById(R.id.etNewRoomName);
        TextInputEditText etJoinRoomCode = dialogView.findViewById(R.id.etJoinRoomCode);

        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setView(dialogView)
                .setPositiveButton("Proses", (dialog, which) -> {
                    String newRoomName = etNewRoomName.getText() != null ? etNewRoomName.getText().toString().trim() : "";
                    String joinRoomCode = etJoinRoomCode.getText() != null ? etJoinRoomCode.getText().toString().trim() : "";

                    if (!newRoomName.isEmpty()) {
                        // Kasus 1: User membuat room baru
                        Toast.makeText(this, "Membuat room: " + newRoomName + " (Proses Backend)", Toast.LENGTH_SHORT).show();
                        // TODO: Bagian backend untuk request API create room
                    } else if (!joinRoomCode.isEmpty()) {
                        // Kasus 2: User bergabung dengan room yang ada
                        Toast.makeText(this, "Bergabung ke kode: " + joinRoomCode + " (Proses Backend)", Toast.LENGTH_SHORT).show();
                        // TODO: Bagian backend untuk request API join room
                    } else {
                        Toast.makeText(this, "Aksi dibatalkan: Kolom input kosong", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCode, tvCount;
        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemRoomName);
            tvCode = itemView.findViewById(R.id.tvItemRoomCode);
            tvCount = itemView.findViewById(R.id.tvItemMemberCount);
        }
    }
}
