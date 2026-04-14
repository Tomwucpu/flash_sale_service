package com.flashsale.user.web;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.auth.RequireRole;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.user.service.UserAuthService;
import com.flashsale.user.service.UserQueryService;
import com.flashsale.user.web.dto.LoginRequest;
import com.flashsale.user.web.dto.LoginResponse;
import com.flashsale.user.web.dto.RegisterRequest;
import com.flashsale.user.web.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserAuthController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final UserAuthService userAuthService;

    private final UserQueryService userQueryService;

    public UserAuthController(UserAuthService userAuthService, UserQueryService userQueryService) {
        this.userAuthService = userAuthService;
        this.userQueryService = userQueryService;
    }

    @PostMapping("/register")
    public ApiResponse<UserProfileResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(requestId(httpServletRequest), userAuthService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(requestId(httpServletRequest), userAuthService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(HttpServletRequest httpServletRequest) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                userQueryService.currentUser(UserContextHolder.get())
        );
    }

    @RequireRole({"ADMIN", "PUBLISHER"})
    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getUserById(
            @PathVariable Long userId,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(requestId(httpServletRequest), userQueryService.getUserById(userId));
    }

    private String requestId(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getHeader(REQUEST_ID_HEADER);
    }
}
