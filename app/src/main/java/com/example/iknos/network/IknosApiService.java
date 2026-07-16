package com.example.iknos.network;

import com.example.iknos.models.ApprovalBody;
import com.example.iknos.models.BaseResponse;
import com.example.iknos.models.CreateRoomRequest;
import com.example.iknos.models.CreateRoomResponse;
import com.example.iknos.models.JoinRoomRequest;
import com.example.iknos.models.LoginRequest;
import com.example.iknos.models.LoginResponse;
import com.example.iknos.models.NoteResponse;
import com.example.iknos.models.RegisterRequest;
import com.example.iknos.models.RequestListResponse;
import com.example.iknos.models.RoomListResponse;
import com.example.iknos.models.UserProfileResponse;
import com.example.iknos.room.RoomDetailResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
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
    // Ambil list orang yang minta join berdasarkan ID Room
    @GET("rooms/{roomId}/requests")
    Call<RequestListResponse> getPendingRequests(@Path("roomId") String roomId);

    // Approve atau Reject request berdasarkan ID Request
    @PATCH("rooms/requests/{requestId}")
    Call<BaseResponse> handleJoinRequest(@Path("requestId") String requestId, @Body ApprovalBody body);

    // Ambil data roomId
    @GET("rooms/{roomId}")
    Call<RoomDetailResponse> getRoomDetail(@Path("roomId") String roomId);

    // Mengambil data profil pengguna
    @GET("users/me")
    Call<UserProfileResponse> getUserProfile();

    // Mengunggah/update foto profil
    @Multipart
    @PUT("users/me/avatar")
    Call<UserProfileResponse> uploadAvatar(
            @Part MultipartBody.Part avatar
    );

    // ─── InstaNote ───────────────────────────────────────────────────────────

    // Upload/update note dengan teks DAN gambar
    @Multipart
    @PUT("notes/{roomId}")
    Call<BaseResponse> upsertNote(
            @Path("roomId") String roomId,
            @Part MultipartBody.Part image,
            @Part("text") RequestBody text
    );

    // Upload/update note hanya teks
    @Multipart
    @PUT("notes/{roomId}")
    Call<BaseResponse> upsertNoteTextOnly(
            @Path("roomId") String roomId,
            @Part("text") RequestBody text
    );

    // Upload/update note hanya gambar
    @Multipart
    @PUT("notes/{roomId}")
    Call<BaseResponse> upsertNoteImageOnly(
            @Path("roomId") String roomId,
            @Part MultipartBody.Part image
    );

    // Ambil semua note anggota room
    @GET("notes/{roomId}")
    Call<NoteResponse> getRoomNotes(@Path("roomId") String roomId);

    // Hapus note sendiri
    @DELETE("notes/{roomId}")
    Call<BaseResponse> deleteNote(@Path("roomId") String roomId);
}