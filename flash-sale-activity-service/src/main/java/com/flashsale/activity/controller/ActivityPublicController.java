package com.flashsale.activity.controller;

import com.flashsale.activity.domain.ActivityPhase;
import com.flashsale.activity.service.ActivityService;
import com.flashsale.activity.dto.response.ActivityDetailResponse;
import com.flashsale.activity.dto.response.ActivityPageResponse;
import com.flashsale.common.core.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/activities")
public class ActivityPublicController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final ActivityService activityService;

    public ActivityPublicController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ApiResponse<ActivityPageResponse> list(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) ActivityPhase phase,
            HttpServletRequest request
    ) {
        return ApiResponse.success(requestId(request), activityService.listPublicActivities(page, size, phase));
    }

    @GetMapping("/{activityId}")
    public ApiResponse<ActivityDetailResponse> detail(@PathVariable Long activityId, HttpServletRequest request) {
        return ApiResponse.success(requestId(request), activityService.getPublicDetail(activityId));
    }

    private String requestId(HttpServletRequest request) {
        return request.getHeader(REQUEST_ID_HEADER);
    }
}
