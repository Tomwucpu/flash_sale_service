package com.flashsale.user.controller;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.auth.RequireRole;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.user.service.UserAuthService;
import com.flashsale.user.service.UserQueryService;
import com.flashsale.user.dto.request.ChangePasswordRequest;
import com.flashsale.user.dto.request.LoginRequest;
import com.flashsale.user.dto.response.LoginResponse;
import com.flashsale.user.dto.request.RegisterRequest;
import com.flashsale.user.dto.request.UpdateProfileRequest;
import com.flashsale.user.dto.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    // 获取当前登录用户信息
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(HttpServletRequest httpServletRequest) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                userQueryService.currentUser(UserContextHolder.get())
        );
    }

    // 根据 userId 查询用户信息
    @PutMapping("/me/profile")
    public ApiResponse<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                userAuthService.updateProfile(UserContextHolder.get(), request)
        );
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpServletRequest
    ) {
        userAuthService.changePassword(UserContextHolder.get(), request);
        return ApiResponse.success(requestId(httpServletRequest), null);
    }

    @RequireRole({"ADMIN", "PUBLISHER"})
    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getUserById(
            @PathVariable Long userId,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(requestId(httpServletRequest), userQueryService.getUserById(userId));
    }

    // 从请求头中获取 X-Request-Id
    private String requestId(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getHeader(REQUEST_ID_HEADER);
    }
}
