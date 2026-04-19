package com.flashsale.payment.web;

import java.math.BigDecimal;

public record PaymentOrderResponse(
        String orderNo,
        String transactionNo,
        BigDecimal payAmount,
        String payStatus
) {
}
