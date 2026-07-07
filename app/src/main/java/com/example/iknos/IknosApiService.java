package com.example.iknos;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface IknosApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterRequest request);
    @POST("rooms")
    Call<CreateRoomResponse> createRoom(@Body CreateRoomRequest request);
    @GET("rooms")
    Call<RoomListResponse> getMyRooms();
    @POST("rooms/join")
    Call<CreateRoomResponse> joinRoom(@Body JoinRoomRequest request);
    // 1. Ambil list orang yang minta join berdasarkan ID Room

    @GET("rooms/{roomId}/requests")
    Call<RequestListResponse> getPendingRequests(@Path("roomId") String roomId);

    // 2. Approve atau Reject request berdasarkan ID Request
    @PATCH("rooms/requests/{requestId}")
    Call<BaseResponse> handleJoinRequest(@Path("requestId") String requestId, @Body ApprovalBody body);

    // Tambahkan endpoint lain di sini nanti
}