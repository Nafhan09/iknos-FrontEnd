package com.example.iknos;

public class JoinRequestModel {
    private String id;        // ID Request (PENTING untuk proses PATCH)
    private String roomId;
    private String userId;
    private String status;    // Biasanya "PENDING"
    private User user;        // Detail info user yang minta join

    public String getId() { return id; }
    public String getStatus() { return status; }
    public User getUser() { return user; }

    public static class User {
        private String username;
        public String getUsername() { return username; }
    }
}