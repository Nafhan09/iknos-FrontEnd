package com.example.iknos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class RoomActivity extends AppCompatActivity {

    private RecyclerView rvRooms;
    private FloatingActionButton fabAddRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        rvRooms = findViewById(R.id.rvRooms);
        fabAddRoom = findViewById(R.id.fabAddRoom);

        // Setup RecyclerView
        rvRooms.setLayoutManager(new LinearLayoutManager(this));

        // Membuat data maksimal 5 room
        List<String[]> mockRoomList = new ArrayList<>();
        mockRoomList.add(new String[]{"Room Liburan Keluarga", "Code: FAM-123", "4/10"});
        mockRoomList.add(new String[]{"Tim Dev Iknos Project", "Code: IKNS-99", "2/10"});
        mockRoomList.add(new String[]{"Koordinasi CFD Jakarta", "Code: CFD-202", "8/10"});

        // Pasang Adapter di dalam kelas
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

                // Klik salah satu room untuk masuk ke map
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(RoomActivity.this, MainActivity.class);
                    // Kirim nama room ke halaman peta
                    intent.putExtra("ROOM_NAME", roomData[0]);
                    startActivity(intent);
                });
            }

            @Override
            public int getItemCount() {
                return mockRoomList.size();
            }
        });

        // Tombol tambah/gabung room
        fabAddRoom.setOnClickListener(v -> {
            Toast.makeText(this, "Fitur Buat/Gabung Room (Teman Backend)", Toast.LENGTH_SHORT).show();
        });
    }

    // ViewHolder class untuk menampung komponen item_room
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
