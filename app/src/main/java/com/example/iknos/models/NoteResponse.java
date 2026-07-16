package com.example.iknos.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model untuk response list note dari GET /api/notes/:roomId
 * Backend mengembalikan array note langsung di field "data".
 */
public class NoteResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<NoteData> data;

    public boolean isSuccess() { return success; }
    public List<NoteData> getData() { return data; }

    public static class NoteData {
        @SerializedName("id")
        private String id;

        @SerializedName("userId")
        private String userId;

        @SerializedName("roomId")
        private String roomId;

        @SerializedName("text")
        private String text;

        @SerializedName("imageUrl")
        private String imageUrl;

        @SerializedName("updatedAt")
        private String updatedAt;

        @SerializedName("user")
        private NoteUser user;

        public String getId() { return id; }
        public String getUserId() { return userId; }
        public String getRoomId() { return roomId; }
        public String getText() { return text; }
        public String getImageUrl() { return imageUrl; }
        public String getUpdatedAt() { return updatedAt; }
        public NoteUser getUser() { return user; }
    }

    public static class NoteUser {
        @SerializedName("id")
        private String id;

        @SerializedName("username")
        private String username;

        @SerializedName("avatarUrl")
        private String avatarUrl;

        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getAvatarUrl() { return avatarUrl; }
    }
}
