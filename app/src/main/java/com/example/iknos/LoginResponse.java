package com.example.iknos;
public class LoginResponse {
    private boolean success;
    private String message;
    private Data data;

    public boolean isSuccess() { return success; }
    public Data getData() { return data; }

    public static class Data {
        private String token; // Ini JWT Token yang kita butuhkan!
        public String getToken() { return token; }
    }
}