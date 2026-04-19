package com.flashsale.activity.web;

import com.flashsale.activity.service.ActivityService;
import com.flashsale.activity.web.dto.ActivityDetailResponse;
import com.flashsale.activity.web.dto.ActivitySummaryResponse;
import com.flashsale.common.core.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/activities")
public class ActivityPublicController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final ActivityService activityService;

    public ActivityPublicController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ApiResponse<List<ActivitySummaryResponse>> list(HttpServletRequest request) {
        return ApiResponse.success(requestId(request), activityService.listPublicActivities());
    }

    @GetMapping("/{activityId}")
    public ApiResponse<ActivityDetailResponse> detail(@PathVariable Long activityId, HttpServletRequest request) {
        return ApiResponse.success(requestId(request), activityService.getPublicDetail(activityId));
    }

    private String requestId(HttpServletRequest request) {
        return request.getHeader(REQUEST_ID_HEADER);
    }
}
