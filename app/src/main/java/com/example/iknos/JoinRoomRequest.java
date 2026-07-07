package com.example.iknos;

public class JoinRoomRequest {
    private String code; // Kode unik room yang diinput user

    public JoinRoomRequest(String code) {
        this.code = code;
    }
}