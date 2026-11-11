package com.cp470.groupproject.data.remote.dto;

/** Body of GET /api/v1/health: {"status":"ok","service":"...","timestamp":"..."} */
public class HealthResponse {
    public String status;
    public String service;
    public String timestamp;
}