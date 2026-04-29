package com.talentcircle.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String message, String error) {
    
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }
    
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, null, message, null);
    }
    
    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, null, null, error);
    }
    
    public static <T> ApiResponse<T> validationError(T errors) {
        return new ApiResponse<>(false, errors, null, "Validation failed");
    }
}
