package com.flashsale.common.web;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.exception.ForbiddenException;
import com.flashsale.common.security.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UnauthorizedException.class)
    public ApiResponse<Void> handleUnauthorized(UnauthorizedException exception, HttpServletRequest request) {
        return ApiResponse.failure("UNAUTHORIZED", exception.getMessage(), request.getHeader(REQUEST_ID_HEADER));
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenException.class)
    public ApiResponse<Void> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
        return ApiResponse.failure("FORBIDDEN", exception.getMessage(), request.getHeader(REQUEST_ID_HEADER));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return ApiResponse.failure("INVALID_ARGUMENT", exception.getMessage(), request.getHeader(REQUEST_ID_HEADER));
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception, HttpServletRequest request) {
        return ApiResponse.failure("SYSTEM_ERROR", exception.getMessage(), request.getHeader(REQUEST_ID_HEADER));
    }
}
