package com.flashsale.order.web;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.auth.RequireRole;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.common.security.exception.UnauthorizedException;
import com.flashsale.order.application.OrderProcessingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final OrderProcessingService orderProcessingService;

    public OrderQueryController(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @GetMapping
    public ApiResponse<List<OrderQueryResponse>> queryOwnOrders(HttpServletRequest request) {
        Long currentUserId = currentUserId();
        List<OrderProcessingService.OrderDetailView> detailViews = orderProcessingService.queryOrdersByUser(currentUserId);
        return ApiResponse.success(
                request.getHeader(REQUEST_ID_HEADER),
                toResponse(detailViews)
        );
    }

    @GetMapping("/activities/{activityId}")
    public ApiResponse<List<OrderQueryResponse>> queryByActivityId(
            @PathVariable Long activityId,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserId();
        List<OrderProcessingService.OrderDetailView> detailViews = orderProcessingService.queryOrdersByActivity(activityId, currentUserId);
        return ApiResponse.success(
                request.getHeader(REQUEST_ID_HEADER),
                toResponse(detailViews)
        );
    }

    @GetMapping("/admin/activities/{activityId}")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<List<OrderQueryResponse>> queryPublisherActivityOrders(
            @PathVariable Long activityId,
            HttpServletRequest request
    ) {
        List<OrderProcessingService.OrderDetailView> detailViews = orderProcessingService.queryPublisherActivityOrders(
                activityId,
                UserContextHolder.get()
        );
        return ApiResponse.success(
                request.getHeader(REQUEST_ID_HEADER),
                toResponse(detailViews)
        );
    }

    private Long currentUserId() {
        UserContext userContext = UserContextHolder.get();
        if (userContext == null || userContext.userId() == null || userContext.userId() <= 0) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }
        return userContext.userId();
    }

    private List<OrderQueryResponse> toResponse(List<OrderProcessingService.OrderDetailView> detailViews) {
        return detailViews.stream()
                .map(detailView -> new OrderQueryResponse(
                        detailView.orderNo(),
                        detailView.activityId(),
                        detailView.userId(),
                        detailView.orderStatus(),
                        detailView.payStatus(),
                        detailView.codeStatus(),
                        detailView.priceAmount(),
                        detailView.failReason(),
                        detailView.code(),
                        detailView.updatedAt()
                ))
                .toList();
    }
}
