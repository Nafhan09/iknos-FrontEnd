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
import com.example.iknos.models.RoomDetailResponse;

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

// INTERFACE/BLUEPRINT SELURUH ENDPOINT YANG DIGUNAKAN
public interface IknosApiService {

    // Endpoint untuk Login dengan method POST
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    // Endpoint untuk Register dengan method POST
    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterRequest request);
    // Endpoint untuk membuat Room dengan method POST
    @POST("rooms")
    Call<CreateRoomResponse> createRoom(@Body CreateRoomRequest request);
    // Endpoint untuk Request List Room dengan method GET
    @GET("rooms")
    Call<RoomListResponse> getMyRooms();
    // Endpoint untuk Mengirim Permintaan Join Room dengan method POST
    @POST("rooms/join")
    Call<CreateRoomResponse> joinRoom(@Body JoinRoomRequest request);
    // Endpoint untuk Request List Pengiriman Permintaan Join Room dengan method POST
    @GET("rooms/{roomId}/requests")
    Call<RequestListResponse> getPendingRequests(@Path("roomId") String roomId);
    // Endpoint untuk Update status Permintaan Join Room dengan method PATCH
    @PATCH("rooms/requests/{requestId}")
    Call<BaseResponse> handleJoinRequest(@Path("requestId") String requestId, @Body ApprovalBody body);
    // Endpoint untuk Request List Room berdasarkan roomId dengan method GET
    @GET("rooms/{roomId}")
    Call<RoomDetailResponse> getRoomDetail(@Path("roomId") String roomId);
    // Endpoint untuk Request Data Pengguna (Diri Sendiri) dengan method GET
    @GET("users/me")
    Call<UserProfileResponse> getUserProfile();
    // Endpoint untuk Update Profil Pengguna dengan method PUT
    @Multipart
    @PUT("users/me/avatar")
    Call<UserProfileResponse> uploadAvatar(@Part MultipartBody.Part avatar);
    // Endpoint untuk Update Note (Gambar dan Teks) dengan method PUT
    @Multipart
    @PUT("notes/{roomId}")
    Call<BaseResponse> upsertNote(@Path("roomId") String roomId, @Part MultipartBody.Part image, @Part("text") RequestBody text);
    // Endpoint untuk Update Note (Teks) dengan method PUT
    @Multipart
    @PUT("notes/{roomId}")
    Call<BaseResponse> upsertNoteTextOnly(@Path("roomId") String roomId, @Part("text") RequestBody text);
    // Endpoint untuk Update Note (Gambar) dengan method PUT
    @Multipart
    @PUT("notes/{roomId}")
    Call<BaseResponse> upsertNoteImageOnly(@Path("roomId") String roomId, @Part MultipartBody.Part image);
    // Endpoint untuk Request list semua note berdasarkan roomId dengan method GET
    @GET("notes/{roomId}")
    Call<NoteResponse> getRoomNotes(@Path("roomId") String roomId);
    // Endpoint untuk Hapus Note berdasarkan roomId dengan method DELETE
    @DELETE("notes/{roomId}")
    Call<BaseResponse> deleteNote(@Path("roomId") String roomId);
}