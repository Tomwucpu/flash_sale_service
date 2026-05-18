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

/**
 * 订单查询控制器
 */
@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final OrderProcessingService orderProcessingService;

    public OrderQueryController(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    /**
     * 查询当前登录用户的“我的订单”列表
     */
    @GetMapping
    public ApiResponse<List<OrderQueryResponse>> queryOwnOrders(HttpServletRequest request) {
        Long currentUserId = currentUserId();
        // 底层服务根据当前用户ID查询所有的历史订单细节视图
        List<OrderProcessingService.OrderDetailView> detailViews = orderProcessingService.queryOrdersByUser(currentUserId);
        return ApiResponse.success(
                request.getHeader(REQUEST_ID_HEADER),
                toResponse(detailViews)
        );
    }

    /**
     * 查询当前用户在特定活动下的订单记录
     */
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

    /**
     * 查询特定活动下的【所有】订单列表
     */
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

    /**
     * 提取并校验当前登录用户的身份（如果无登录态则直接抛权限异常拦截）
     */
    private Long currentUserId() {
        UserContext userContext = UserContextHolder.get();
        if (userContext == null || userContext.userId() == null || userContext.userId() <= 0) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }
        return userContext.userId();
    }

    /**
     * 将服务层输出的视图 DTO (OrderDetailView) 转化为面向 Web 或前端的响应装载类 (OrderQueryResponse)
     */
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
