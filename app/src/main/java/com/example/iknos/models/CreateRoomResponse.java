package com.example.iknos.models;

public class CreateRoomResponse {
    private boolean success;
    private String message;
    private RoomModel data; // Menggunakan objek RoomModel yang sudah dibuat sebelumnya

    public boolean isSuccess() { return success; }
    public RoomModel getData() { return data; }
}