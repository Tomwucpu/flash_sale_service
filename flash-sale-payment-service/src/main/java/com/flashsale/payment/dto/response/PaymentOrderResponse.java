package com.flashsale.payment.dto.response;

import java.math.BigDecimal;

public record PaymentOrderResponse(
        String orderNo,
        String transactionNo,
        BigDecimal payAmount,
        String payStatus
) {
}
