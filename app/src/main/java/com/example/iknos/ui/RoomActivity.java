package com.example.iknos.ui;

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

import com.example.iknos.network.IknosApiService;
import com.example.iknos.models.CreateRoomRequest;
import com.example.iknos.models.CreateRoomResponse;

public class RoomActivity extends AppCompatActivity {

    private RecyclerView rvRooms;
    private FloatingActionButton fabAddRoom;
    private Button btnLogout;
    private final List<RoomModel> roomList = new ArrayList<>();
    private RecyclerView.Adapter<RoomViewHolder> roomAdapter;
    private Button btnJoinRequests;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        rvRooms = findViewById(R.id.rvRooms);
        fabAddRoom = findViewById(R.id.fabAddRoom);
        btnLogout = findViewById(R.id.btnLogout);
        btnJoinRequests = findViewById(R.id.btnJoinRequests);

        btnJoinRequests.setOnClickListener(v -> {

            if (roomList.isEmpty()) {

                Toast.makeText(
                        RoomActivity.this,
                        "Belum ada room",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String roomId = roomList.get(0).getId();

            loadPendingRequests(roomId);

        });

        // Setup RecyclerView
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

                // sementara backend belum mengirim jumlah member
                holder.tvCount.setText("-");

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(RoomActivity.this, MainActivity.class);
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

                    String roomNameInput = etNewRoomName.getText() != null
                            ? etNewRoomName.getText().toString().trim()
                            : "";

                    String joinRoomCode = etJoinRoomCode.getText() != null
                            ? etJoinRoomCode.getText().toString().trim()
                            : "";

                    // ==========================
                    // CREATE ROOM
                    // ==========================
                    if (!roomNameInput.isEmpty()) {

                        IknosApiService apiService =
                                RetrofitClient.getClient(RoomActivity.this)
                                        .create(IknosApiService.class);

                        CreateRoomRequest request =
                                new CreateRoomRequest(roomNameInput);

                        apiService.createRoom(request).enqueue(new Callback<CreateRoomResponse>() {

                            @Override
                            public void onResponse(Call<CreateRoomResponse> call,
                                                   Response<CreateRoomResponse> response) {

                                if (response.isSuccessful()
                                        && response.body() != null
                                        && response.body().isSuccess()) {

                                    RoomModel newRoom = response.body().getData();

                                    Toast.makeText(
                                            RoomActivity.this,
                                            "Room berhasil dibuat!\nKode: " + newRoom.getCode(),
                                            Toast.LENGTH_LONG
                                    ).show();

                                    // Refresh daftar room
                                    fetchRealRooms();

                                } else {

                                    Toast.makeText(
                                            RoomActivity.this,
                                            "Gagal membuat room. Maksimal 5 room.",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                }
                            }

                            @Override
                            public void onFailure(Call<CreateRoomResponse> call,
                                                  Throwable t) {

                                Toast.makeText(
                                        RoomActivity.this,
                                        "Error koneksi: " + t.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();

                            }
                        });

                    }

                    // ==========================
                    // JOIN ROOM
                    // ==========================
                    else if (!joinRoomCode.isEmpty()) {

                        IknosApiService apiService =
                                RetrofitClient.getClient(RoomActivity.this)
                                        .create(IknosApiService.class);

                        JoinRoomRequest request = new JoinRoomRequest(joinRoomCode);

                        apiService.joinRoom(request).enqueue(new Callback<CreateRoomResponse>() {

                            @Override
                            public void onResponse(Call<CreateRoomResponse> call,
                                                   Response<CreateRoomResponse> response) {

                                if (response.isSuccessful()
                                        && response.body() != null
                                        && response.body().isSuccess()) {

                                    Toast.makeText(
                                            RoomActivity.this,
                                            "Permintaan bergabung dikirim.\nMenunggu persetujuan Owner!",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    fetchRealRooms();

                                } else {

                                    Toast.makeText(
                                            RoomActivity.this,
                                            "Gagal join room. Cek kembali kode room.",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                }

                            }

                            @Override
                            public void onFailure(Call<CreateRoomResponse> call,
                                                  Throwable t) {

                                Toast.makeText(
                                        RoomActivity.this,
                                        "Error: " + t.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();

                            }

                        });

                    }

                    else {

                        Toast.makeText(
                                RoomActivity.this,
                                "Nama room atau kode room harus diisi!",
                                Toast.LENGTH_SHORT
                        ).show();

                    }

                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }



    private void fetchRealRooms() {

        IknosApiService apiService =
                RetrofitClient.getClient(RoomActivity.this)
                        .create(IknosApiService.class);

        apiService.getMyRooms().enqueue(new Callback<RoomListResponse>() {

            @Override
            public void onResponse(Call<RoomListResponse> call,
                                   Response<RoomListResponse> response) {

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()) {

                    roomList.clear();
                    roomList.addAll(response.body().getData());

                    roomAdapter.notifyDataSetChanged();

                } else {

                    Toast.makeText(RoomActivity.this,
                            "Gagal mengambil daftar room",
                            Toast.LENGTH_SHORT).show();

                }

            }

            @Override
            public void onFailure(Call<RoomListResponse> call,
                                  Throwable t) {

                Toast.makeText(RoomActivity.this,
                        t.getMessage(),
                        Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void loadPendingRequests(String roomId) {

        IknosApiService apiService =
                RetrofitClient.getClient(RoomActivity.this)
                        .create(IknosApiService.class);

        apiService.getPendingRequests(roomId)
                .enqueue(new Callback<RequestListResponse>() {

                    @Override
                    public void onResponse(Call<RequestListResponse> call,
                                           Response<RequestListResponse> response) {

                        if(response.isSuccessful()
                                && response.body()!=null
                                && response.body().isSuccess()){

                            List<JoinRequestModel> requests =
                                    response.body().getData();

                            showRequestDialog(requests);

                        }

                    }

                    @Override
                    public void onFailure(Call<RequestListResponse> call,
                                          Throwable t) {

                        Toast.makeText(
                                RoomActivity.this,
                                t.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();

                    }

                });

    }


    private void showRequestDialog(List<JoinRequestModel> requests) {

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_request, null);

        RecyclerView rvRequests = dialogView.findViewById(R.id.rvRequests);

        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        RequestAdapter adapter =
                new RequestAdapter(this, requests);

        rvRequests.setAdapter(adapter);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Permintaan Bergabung")
                .setView(dialogView)
                .setNegativeButton("Tutup", null)
                .show();
    }

    public void approveOrRejectUser(String requestId,
                                    String actionName,
                                    String roomId) {

        IknosApiService apiService =
                RetrofitClient.getClient(RoomActivity.this)
                        .create(IknosApiService.class);

        ApprovalBody body = new ApprovalBody(actionName);

        apiService.handleJoinRequest(requestId, body)
                .enqueue(new Callback<BaseResponse>() {

                    @Override
                    public void onResponse(Call<BaseResponse> call,
                                           Response<BaseResponse> response) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            Toast.makeText(
                                    RoomActivity.this,
                                    "Request berhasil di-" + actionName,
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadPendingRequests(roomId);
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call,
                                          Throwable t) {

                        Toast.makeText(
                                RoomActivity.this,
                                "Error: " + t.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();

                    }

                });
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
