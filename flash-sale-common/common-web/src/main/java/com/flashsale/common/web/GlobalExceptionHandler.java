package com.flashsale.common.web;

import com.flashsale.common.core.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return ApiResponse.failure("INVALID_ARGUMENT", exception.getMessage(), request.getHeader(REQUEST_ID_HEADER));
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception, HttpServletRequest request) {
        return ApiResponse.failure("SYSTEM_ERROR", exception.getMessage(), request.getHeader(REQUEST_ID_HEADER));
    }
}
