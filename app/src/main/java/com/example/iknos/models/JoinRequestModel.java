package com.example.iknos.models;

public class JoinRequestModel {

    private String id;
    private String roomId;
    private String userId;
    private String status;
    private User user;

    public String getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public User getUser() {
        return user;
    }

    public static class User {
        private String username;

        public String getUsername() {
            return username;
        }
    }
}