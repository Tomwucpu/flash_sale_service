package com.flashsale.order.application;

import com.flashsale.common.mq.event.DomainEvent;

import java.util.Map;

public record PaymentSuccessPayload(String orderNo, String transactionNo) {

    public static PaymentSuccessPayload from(DomainEvent<?> event) {
        if (event == null) {
            throw new IllegalArgumentException("payment.success 事件不能为空");
        }
        if (!(event.payload() instanceof Map<?, ?> rawPayload)) {
            throw new IllegalArgumentException("payment.success 事件载荷格式非法");
        }
        return new PaymentSuccessPayload(
                stringValue(rawPayload.get("orderNo"), "orderNo"),
                stringValue(rawPayload.get("transactionNo"), "transactionNo")
        );
    }

    private static String stringValue(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("payment.success 缺少字段: " + fieldName);
        }
        String actual = String.valueOf(value);
        if (actual.isBlank()) {
            throw new IllegalArgumentException("payment.success 字段为空: " + fieldName);
        }
        return actual;
    }
}
