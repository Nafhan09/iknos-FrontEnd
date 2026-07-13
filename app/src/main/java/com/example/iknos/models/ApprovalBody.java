package com.example.iknos.models;

public class ApprovalBody {
    private String action; // Isinya wajib "approve" atau "reject"

    public ApprovalBody(String action) {
        this.action = action;
    }
}