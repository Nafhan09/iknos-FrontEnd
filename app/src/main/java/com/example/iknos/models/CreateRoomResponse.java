package com.example.iknos.models;

public class CreateRoomResponse {
    private boolean success;
    private String message;
    private RoomModel data;

    public boolean isSuccess() { return success; }
    public RoomModel getData() { return data; }
}