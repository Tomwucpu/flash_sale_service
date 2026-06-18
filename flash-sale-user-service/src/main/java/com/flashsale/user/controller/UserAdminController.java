package com.flashsale.user.controller;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.auth.RequireRole;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.user.dto.request.UserPageRequest;
import com.flashsale.user.dto.request.UserRoleRequest;
import com.flashsale.user.dto.request.UserStatusRequest;
import com.flashsale.user.dto.response.UserPageResponse;
import com.flashsale.user.dto.response.UserProfileResponse;
import com.flashsale.user.service.UserAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    @RequireRole({"ADMIN"})
    public ApiResponse<UserPageResponse> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest httpServletRequest
    ) {
        UserPageRequest request = new UserPageRequest(keyword, role, status, page, size);
        return ApiResponse.success(requestId(httpServletRequest), userAdminService.listUsers(request));
    }

    @PutMapping("/{userId}/status")
    @RequireRole({"ADMIN"})
    public ApiResponse<UserProfileResponse> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                userAdminService.updateStatus(userId, request.status(), UserContextHolder.get())
        );
    }

    @PutMapping("/{userId}/role")
    @RequireRole({"ADMIN"})
    public ApiResponse<UserProfileResponse> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                userAdminService.updateRole(userId, request.role(), UserContextHolder.get())
        );
    }

    private String requestId(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getHeader(REQUEST_ID_HEADER);
    }
}
