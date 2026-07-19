package com.example.iknos.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iknos.R;
import com.example.iknos.models.RoomModel;
import com.example.iknos.models.ApprovalBody;
import com.example.iknos.models.BaseResponse;
import com.example.iknos.models.JoinRequestModel;
import com.example.iknos.models.JoinRoomRequest;
import com.example.iknos.models.RequestListResponse;
import com.example.iknos.models.RoomListResponse;
import com.example.iknos.network.RetrofitClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.SharedPreferences;

import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;
import com.example.iknos.models.UserProfileResponse;

import com.example.iknos.network.IknosApiService;
import com.example.iknos.models.CreateRoomRequest;
import com.example.iknos.models.CreateRoomResponse;

public class RoomActivity extends AppCompatActivity {

    private RecyclerView rvRooms;
    private FloatingActionButton fabAddRoom;
    private final List<RoomModel> roomList = new ArrayList<>();
    private RecyclerView.Adapter<RoomViewHolder> roomAdapter;
    private ImageButton btnJoinRequests;
    private CircleImageView ivSettingsAvatar;

    // Method untuk inisialisasi RoomActivity saat dibuka
    // Method meliputi layouting, event untuk SettingsActivity, Logika Join Request, Intent MainActivity dengan putExtra Name & id room dan pemanggilan fungsi fetchRealRooms() untuk mengambil data room terbaru dari server/database.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        rvRooms = findViewById(R.id.rvRooms);
        fabAddRoom = findViewById(R.id.fabAddRoom);
        btnJoinRequests = findViewById(R.id.btnJoinRequests);
        ivSettingsAvatar = findViewById(R.id.ivSettingsAvatar);

        ivSettingsAvatar.setOnClickListener(v -> {Intent intent = new Intent(RoomActivity.this, SettingsActivity.class);startActivity(intent);});

        btnJoinRequests.setOnClickListener(v -> {
            if (roomList.isEmpty()) {
                // Tampilkan dialog kosong jika pengguna belum memiliki room
                showRequestDialog(new ArrayList<>(), "");
                return;
            }

            String roomId = roomList.get(0).getId();
            String roomName = roomList.get(0).getName();
            loadPendingRequests(roomId, roomName);
        });

        rvRooms.setLayoutManager(new LinearLayoutManager(this));

        roomAdapter = new RecyclerView.Adapter<RoomViewHolder>() {
            @NonNull
            @Override
            public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
                return new RoomViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
                RoomModel roomData = roomList.get(position);
                holder.tvName.setText(roomData.getName());
                holder.tvCode.setText("Code: " + roomData.getCode());
                holder.tvCount.setText(roomData.getMemberCount() + " anggota");

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(RoomActivity.this, MainActivity.class);
                    // putExtra() -> menyelipkan data ke dalam objek Intent sebelum Anda berpindah halaman.
                    intent.putExtra("ROOM_NAME", roomData.getName());
                    intent.putExtra("ROOM_ID", roomData.getId());
                    startActivity(intent);
                });
            }

            @Override
            public int getItemCount() {
                return roomList.size();
            }
        };

        rvRooms.setAdapter(roomAdapter);
        fetchRealRooms();

        fabAddRoom.setOnClickListener(v -> showAddRoomDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserProfile(); // Perbarui avatar setiap kali halaman ini kembali aktif
    }

    private void fetchUserProfile() {
        IknosApiService apiService = RetrofitClient.getClient(this).create(IknosApiService.class);
        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    String avatarUrl = response.body().getData().getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(RoomActivity.this)
                                .load(avatarUrl)
                                .placeholder(R.mipmap.ic_launcher_round)
                                .into(ivSettingsAvatar);
                    }
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                // Jika gagal, biarkan menggunakan gambar default
            }
        });
    }

    // Fungsi modal untuk Create/Join Room
    // Fungsi meliputi layouting, pengkondisian create/join room dan event batal/proses
    private void showAddRoomDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_room, null);

        // View referensi
        TextView tabCreate = dialogView.findViewById(R.id.tabCreate);
        TextView tabJoin   = dialogView.findViewById(R.id.tabJoin);
        View panelCreate   = dialogView.findViewById(R.id.panelCreate);
        View panelJoin     = dialogView.findViewById(R.id.panelJoin);
        TextInputEditText etNewRoomName  = dialogView.findViewById(R.id.etNewRoomName);
        TextInputEditText etJoinRoomCode = dialogView.findViewById(R.id.etJoinRoomCode);
        Button btnCreateRoom = dialogView.findViewById(R.id.btnCreateRoom);
        Button btnJoinRoom   = dialogView.findViewById(R.id.btnJoinRoom);

        // Buat dialog tanpa tombol positif/negatif bawaan
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setView(dialogView)
                .create();

        // Helper: switch tampilan tab
        Runnable showCreate = () -> {
            panelCreate.setVisibility(View.VISIBLE);
            panelJoin.setVisibility(View.GONE);
            tabCreate.setTextColor(getColor(android.R.color.holo_green_light));
            tabCreate.setBackgroundResource(R.drawable.tab_underline_selected);
            tabJoin.setTextColor(0xFF888888);
            tabJoin.setBackgroundResource(R.drawable.tab_underline_unselected);
        };
        Runnable showJoin = () -> {
            panelCreate.setVisibility(View.GONE);
            panelJoin.setVisibility(View.VISIBLE);
            tabJoin.setTextColor(getColor(android.R.color.holo_green_light));
            tabJoin.setBackgroundResource(R.drawable.tab_underline_selected);
            tabCreate.setTextColor(0xFF888888);
            tabCreate.setBackgroundResource(R.drawable.tab_underline_unselected);
        };

        tabCreate.setOnClickListener(v -> showCreate.run());
        tabJoin.setOnClickListener(v -> showJoin.run());

        // --- Aksi tombol Create Room ---
        btnCreateRoom.setOnClickListener(v -> {
            String roomName = etNewRoomName.getText() != null ? etNewRoomName.getText().toString().trim() : "";
            if (roomName.isEmpty()) {
                Toast.makeText(this, "Nama ruangan tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                return;
            }
            IknosApiService api = RetrofitClient.getClient(RoomActivity.this).create(IknosApiService.class);
            api.createRoom(new CreateRoomRequest(roomName)).enqueue(new Callback<CreateRoomResponse>() {
                @Override
                public void onResponse(Call<CreateRoomResponse> call, Response<CreateRoomResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        RoomModel newRoom = response.body().getData();
                            // TODO: HAPUS/GANTI TOAST
                        Toast.makeText(RoomActivity.this, "Room berhasil dibuat!\nKode: " + newRoom.getCode(), Toast.LENGTH_LONG).show();

                            // Panggil fungsi untuk mengambil daftar Room sekaligus Refresh data yang ditampilkan
                        fetchRealRooms();
                        dialog.dismiss();
                    } else {
                            // TODO: HAPUS/GANTI TOAST
                        Toast.makeText(RoomActivity.this, "Gagal membuat room. Maksimal 5 room.", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<CreateRoomResponse> call, Throwable t) {
                        // TODO: HAPUS/GANTI TOAST
                    Toast.makeText(RoomActivity.this, "Error koneksi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // --- Aksi tombol Join Room ---
        btnJoinRoom.setOnClickListener(v -> {
            String roomCode = etJoinRoomCode.getText() != null ? etJoinRoomCode.getText().toString().trim() : "";
            if (roomCode.isEmpty()) {
                Toast.makeText(this, "Kode ruangan tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                return;
            }
            IknosApiService api = RetrofitClient.getClient(RoomActivity.this).create(IknosApiService.class);
            api.joinRoom(new JoinRoomRequest(roomCode)).enqueue(new Callback<CreateRoomResponse>() {
                @Override
                public void onResponse(Call<CreateRoomResponse> call, Response<CreateRoomResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(RoomActivity.this, "Permintaan bergabung dikirim.\nMenunggu persetujuan Owner!", Toast.LENGTH_LONG).show();
                        fetchRealRooms();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(RoomActivity.this, "Gagal join room. Cek kembali kode room.", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<CreateRoomResponse> call, Throwable t) {
                    Toast.makeText(RoomActivity.this, "Error koneksi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    // Fungsi Request List Room
    private void fetchRealRooms() {

        // Jalankan Retrofit
        IknosApiService apiService = RetrofitClient.getClient(RoomActivity.this).create(IknosApiService.class);

        apiService.getMyRooms().enqueue(new Callback<RoomListResponse>() {
            @Override
            public void onResponse(Call<RoomListResponse> call, Response<RoomListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    roomList.clear();
                    roomList.addAll(response.body().getData());
                    roomAdapter.notifyDataSetChanged();
                } else {
                    // TODO: HAPUS/GANTI TOAST
                    Toast.makeText(RoomActivity.this, "Gagal mengambil daftar room", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RoomListResponse> call, Throwable t) {
                // TODO: HAPUS/GANTI TOAST
                Toast.makeText(RoomActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Fungsi Request List Pending Request (Permintaan Join Room)
    private void loadPendingRequests(String roomId, String roomName) {
        IknosApiService apiService = RetrofitClient.getClient(RoomActivity.this).create(IknosApiService.class);

        apiService.getPendingRequests(roomId).enqueue(new Callback<RequestListResponse>() {
            @Override
            public void onResponse(Call<RequestListResponse> call, Response<RequestListResponse> response) {
                if(response.isSuccessful() && response.body()!=null && response.body().isSuccess()){
                    List<JoinRequestModel> requests = response.body().getData();

                    showRequestDialog(requests, roomName);
                }
            }

            @Override
            public void onFailure(Call<RequestListResponse> call, Throwable t) {
                // TODO: HAPUS/GANTI TOAST
                Toast.makeText(RoomActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Fungsi Modal untuk Pending Request
    private void showRequestDialog(List<JoinRequestModel> requests, String roomName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_request, null);

        RecyclerView rvRequests = dialogView.findViewById(R.id.rvRequests);

        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        RequestAdapter adapter = new RequestAdapter(this, requests, roomName);

        rvRequests.setAdapter(adapter);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_dialog_dark))
                .setNegativeButton("Tutup", null)
                .show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#00E676"));
    }

    // Fungsi pemrosesan Pending Request Join Room
    // Fungsi meliputi pemanggilan fungsi loadPendingRequests(roomId) untuk mendapatkan data terbaru dan accept/reject request
    public void approveOrRejectUser(String requestId, String actionName, String roomId, String roomName) {
        IknosApiService apiService = RetrofitClient.getClient(RoomActivity.this).create(IknosApiService.class);

        ApprovalBody body = new ApprovalBody(actionName);

        apiService.handleJoinRequest(requestId, body).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // TODO: HAPUS/GANTI TOAST
                    Toast.makeText(RoomActivity.this, "Request berhasil di-" + actionName, Toast.LENGTH_SHORT).show();

                    loadPendingRequests(roomId, roomName);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                // TODO: HAPUS/GANTI TOAST
                Toast.makeText(RoomActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ViewHolder Class untuk caching referensi komponen UI
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
