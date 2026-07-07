package com.example.iknos;

import java.util.List;

public class RequestListResponse {
    private boolean success;
    private List<JoinRequestModel> data;

    public boolean isSuccess() { return success; }
    public List<JoinRequestModel> getData() { return data; }
}