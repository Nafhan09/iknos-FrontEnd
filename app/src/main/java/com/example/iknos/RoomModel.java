package com.example.iknos;

public class RoomModel {
    private String id;
    private String name;
    private String code;
    private String ownerId;

    // Getter untuk kebutuhan RecyclerView
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getOwnerId() { return ownerId; }
}