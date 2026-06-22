package com.flashsale.user.controller;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.auth.RequireRole;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.user.dto.request.ApplicationPageRequest;
import com.flashsale.user.dto.request.ApplicationReviewRequest;
import com.flashsale.user.dto.request.UserPageRequest;
import com.flashsale.user.dto.request.UserRoleRequest;
import com.flashsale.user.dto.request.UserStatusRequest;
import com.flashsale.user.dto.response.ApplicationPageResponse;
import com.flashsale.user.dto.response.PublisherApplicationResponse;
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

    @GetMapping("/publisher-applications")
    @RequireRole({"ADMIN"})
    public ApiResponse<ApplicationPageResponse> listApplications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest httpServletRequest
    ) {
        ApplicationPageRequest request = new ApplicationPageRequest(status, page, size);
        return ApiResponse.success(
                requestId(httpServletRequest),
                userAdminService.listApplications(request)
        );
    }

    @PutMapping("/publisher-applications/{applicationId}/approve")
    @RequireRole({"ADMIN"})
    public ApiResponse<PublisherApplicationResponse> approveApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody(required = false) ApplicationReviewRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String reviewNote = request != null ? request.reviewNote() : null;
        return ApiResponse.success(
                requestId(httpServletRequest),
                userAdminService.approveApplication(applicationId, reviewNote, UserContextHolder.get())
        );
    }

    @PutMapping("/publisher-applications/{applicationId}/reject")
    @RequireRole({"ADMIN"})
    public ApiResponse<PublisherApplicationResponse> rejectApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody(required = false) ApplicationReviewRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String reviewNote = request != null ? request.reviewNote() : null;
        return ApiResponse.success(
                requestId(httpServletRequest),
                userAdminService.rejectApplication(applicationId, reviewNote, UserContextHolder.get())
        );
    }

    private String requestId(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getHeader(REQUEST_ID_HEADER);
    }
}
