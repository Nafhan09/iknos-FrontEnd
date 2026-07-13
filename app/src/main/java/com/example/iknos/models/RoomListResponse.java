package com.example.iknos.models;

import java.util.List;

public class RoomListResponse {

    private boolean success;
    private String message;
    private List<RoomModel> data;

    public boolean isSuccess() {
        return success;
    }

    public List<RoomModel> getData() {
        return data;
    }
}