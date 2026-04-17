package com.flashsale.activity.web;

import com.flashsale.activity.service.ActivityService;
import com.flashsale.activity.web.dto.ActivityCreateRequest;
import com.flashsale.activity.web.dto.ActivityDetailResponse;
import com.flashsale.activity.web.dto.ActivitySummaryResponse;
import com.flashsale.activity.web.dto.ActivityUpdateRequest;
import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.auth.RequireRole;
import com.flashsale.common.security.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityAdminController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final ActivityService activityService;

    public ActivityAdminController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ActivityDetailResponse> create(
            @Valid @RequestBody ActivityCreateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                activityService.create(request, UserContextHolder.get())
        );
    }

    @PutMapping("/{activityId}")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ActivityDetailResponse> update(
            @PathVariable Long activityId,
            @Valid @RequestBody ActivityUpdateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                activityService.update(activityId, request, UserContextHolder.get())
        );
    }

    @GetMapping("/{activityId}")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ActivityDetailResponse> detail(
            @PathVariable Long activityId,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(requestId(httpServletRequest), activityService.getDetail(activityId));
    }

    @GetMapping
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<List<ActivitySummaryResponse>> list(HttpServletRequest httpServletRequest) {
        return ApiResponse.success(requestId(httpServletRequest), activityService.list());
    }

    @PostMapping("/{activityId}/publish")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ActivityDetailResponse> publish(
            @PathVariable Long activityId,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                activityService.publish(activityId, UserContextHolder.get())
        );
    }

    @PostMapping("/{activityId}/offline")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ActivityDetailResponse> offline(
            @PathVariable Long activityId,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                activityService.offline(activityId, UserContextHolder.get())
        );
    }

    @DeleteMapping("/{activityId}")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<Void> delete(
            @PathVariable Long activityId,
            HttpServletRequest httpServletRequest
    ) {
        activityService.delete(activityId, UserContextHolder.get());
        return ApiResponse.success(requestId(httpServletRequest), null);
    }

    private String requestId(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getHeader(REQUEST_ID_HEADER);
    }
}
