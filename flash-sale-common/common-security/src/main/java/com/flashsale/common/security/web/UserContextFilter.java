package com.flashsale.common.security.web;

import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.context.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            if (hasUserHeader(request)) {
                UserContextHolder.set(UserContext.fromHeaders(extractHeaders(request)));
            } else {
                UserContextHolder.clear();
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private boolean hasUserHeader(HttpServletRequest request) {
        return request.getHeader(UserContext.USER_ID_HEADER) != null;
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        headers.put(UserContext.USER_ID_HEADER, request.getHeader(UserContext.USER_ID_HEADER));
        headers.put(UserContext.USERNAME_HEADER, request.getHeader(UserContext.USERNAME_HEADER));
        headers.put(UserContext.ROLE_HEADER, request.getHeader(UserContext.ROLE_HEADER));
        return headers;
    }
}
