package com.flashsale.payment.web;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.common.security.exception.UnauthorizedException;
import com.flashsale.payment.application.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderNo}")
    public ApiResponse<PaymentOrderResponse> createPayment(
            @PathVariable String orderNo,
            HttpServletRequest request
    ) {
        UserContext userContext = UserContextHolder.get();
        if (userContext == null || userContext.userId() == null || userContext.userId() <= 0) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }
        PaymentService.PaymentOrderView paymentOrderView = paymentService.createPayment(orderNo, userContext.userId());
        return ApiResponse.success(
                request.getHeader(REQUEST_ID_HEADER),
                new PaymentOrderResponse(
                        paymentOrderView.orderNo(),
                        paymentOrderView.transactionNo(),
                        paymentOrderView.payAmount(),
                        paymentOrderView.payStatus()
                )
        );
    }

    @PostMapping("/callback")
    public ApiResponse<PaymentOrderResponse> callback(
            @Valid @RequestBody PaymentCallbackRequest requestBody,
            HttpServletRequest request
    ) {
        Map<String, Object> callbackPayload = new LinkedHashMap<>();
        callbackPayload.put("orderNo", requestBody.getOrderNo());
        callbackPayload.put("transactionNo", requestBody.getTransactionNo());

        PaymentService.PaymentOrderView paymentOrderView = paymentService.handleCallback(
                requestBody.getOrderNo(),
                requestBody.getTransactionNo(),
                callbackPayload
        );
        return ApiResponse.success(
                request.getHeader(REQUEST_ID_HEADER),
                new PaymentOrderResponse(
                        paymentOrderView.orderNo(),
                        paymentOrderView.transactionNo(),
                        paymentOrderView.payAmount(),
                        paymentOrderView.payStatus()
                )
        );
    }
}
