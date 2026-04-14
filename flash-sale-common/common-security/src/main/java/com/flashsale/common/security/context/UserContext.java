package com.flashsale.common.security.context;

import java.util.Map;
import java.util.Objects;

public record UserContext(Long userId, String username, String role) {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USERNAME_HEADER = "X-Username";
    public static final String ROLE_HEADER = "X-Role";

    public static UserContext fromHeaders(Map<String, String> headers) {
        Objects.requireNonNull(headers, "headers must not be null");
        return new UserContext(
                Long.parseLong(headers.getOrDefault(USER_ID_HEADER, "0")),
                headers.getOrDefault(USERNAME_HEADER, ""),
                headers.getOrDefault(ROLE_HEADER, "")
        );
    }

    public Map<String, String> toHeaders() {
        return Map.of(
                USER_ID_HEADER, String.valueOf(userId),
                USERNAME_HEADER, username,
                ROLE_HEADER, role
        );
    }
}
