package com.cp470.groupproject.data.remote.dto;

import java.util.List;

/** Every backend error looks like {"error":{"code":"...","message":"...","details":[...]}} */
public class ApiErrorResponse {
    public ErrorBody error;

    public static class ErrorBody {
        public String code;          // stable, e.g. "VALIDATION_ERROR", "AI_TIMEOUT", "RATE_LIMITED"
        public String message;       // human-readable (English)
        public List<String> details; // only for validation errors
    }
}