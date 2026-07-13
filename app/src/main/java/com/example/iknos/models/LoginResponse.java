package com.example.iknos.models;

public class LoginResponse {
    private boolean success;
    private String message;
    private Data data;

    public boolean isSuccess() { return success; }
    public Data getData() { return data; }

    public static class Data {
        private String token; // Ini JWT Token yang kita butuhkan!
        private User user;
        
        public String getToken() { return token; }
        public User getUser() { return user; }
    }

    public static class User {
        private String id;
        public String getId() { return id; }
    }
}