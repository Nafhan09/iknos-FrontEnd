package com.example.iknos.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RoomDetailResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public RoomData data;

    public static class RoomData {
        @SerializedName("id")
        public String id;

        @SerializedName("name")
        public String name;

        @SerializedName("code")
        public String code;

        @SerializedName("members")
        public List<RoomMember> members;
    }

    public static class RoomMember {
        @SerializedName("userId")
        public String userId;

        @SerializedName("isHidden")
        public boolean isHidden;

        @SerializedName("lastLat")
        public Double lastLat;

        @SerializedName("lastLng")
        public Double lastLng;

        @SerializedName("user")
        public UserInfo user;
    }

    public static class UserInfo {
        @SerializedName("id")
        public String id;

        @SerializedName("username")
        public String username;

        @SerializedName("avatarUrl")
        public String avatarUrl;
    }
}