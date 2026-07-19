package com.example.iknos.models;

import com.google.gson.annotations.SerializedName;

public class UpdateUsernameRequest {
    @SerializedName("username")
    private final String username;

    public UpdateUsernameRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
