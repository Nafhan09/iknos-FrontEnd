package com.example.iknos.models;

import com.google.gson.annotations.SerializedName;

public class RoomModel {

    @SerializedName("roomId")
    private String id;

    private String name;

    private String code;

    private String ownerId;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getOwnerId() {
        return ownerId;
    }
}