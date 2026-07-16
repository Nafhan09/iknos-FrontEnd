package com.example.iknos.models;

import com.google.gson.annotations.SerializedName;

public class UserProfileResponse extends BaseResponse {
    @SerializedName("data")
    private UserData data;

    public UserData getData() {
        return data;
    }

    public static class UserData {
        @SerializedName("id")
        private String id;
        @SerializedName("username")
        private String username;
        @SerializedName("email")
        private String email;
        @SerializedName("avatarUrl")
        private String avatarUrl;

        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getAvatarUrl() { return avatarUrl; }
    }
}