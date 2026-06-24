package com.flashsale.order.controller;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.auth.RequireRole;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.common.security.exception.UnauthorizedException;
import com.flashsale.order.dto.response.PublisherDashboardResponse;
import com.flashsale.order.service.PublisherDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/publisher")
public class PublisherDashboardController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final PublisherDashboardService dashboardService;

    public PublisherDashboardController(PublisherDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<PublisherDashboardResponse> getDashboard(
            @RequestParam(value = "granularity", required = false) String granularity,
            HttpServletRequest request
    ) {
        Long publisherId = currentUserId();
        PublisherDashboardResponse dashboard = dashboardService.getDashboard(publisherId, granularity);
        return ApiResponse.success(request.getHeader(REQUEST_ID_HEADER), dashboard);
    }

    private Long currentUserId() {
        UserContext userContext = UserContextHolder.get();
        if (userContext == null || userContext.userId() == null || userContext.userId() <= 0) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }
        return userContext.userId();
    }
}
