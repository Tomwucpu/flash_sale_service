package com.flashsale.order.web;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.common.security.exception.UnauthorizedException;
import com.flashsale.order.application.OrderProcessingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/codes")
public class OrderCodeController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final OrderProcessingService orderProcessingService;

    public OrderCodeController(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<OrderCodeResponse> queryByOrderNo(
            @PathVariable String orderNo,
            HttpServletRequest request
    ) {
        UserContext userContext = UserContextHolder.get();
        if (userContext == null || userContext.userId() == null || userContext.userId() <= 0) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }
        OrderProcessingService.OrderCodeView orderCodeView = orderProcessingService.queryOrderCode(orderNo, userContext.userId());
        return ApiResponse.success(
                request.getHeader(REQUEST_ID_HEADER),
                new OrderCodeResponse(
                        orderCodeView.orderNo(),
                        orderCodeView.activityId(),
                        orderCodeView.orderStatus(),
                        orderCodeView.payStatus(),
                        orderCodeView.codeStatus(),
                        orderCodeView.code(),
                        orderCodeView.updatedAt()
                )
        );
    }
}
