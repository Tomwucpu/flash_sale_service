package com.flashsale.order.application;

import com.flashsale.common.mq.event.DomainEvent;

import java.util.Map;

public record OrderTimeoutClosePayload(String orderNo) {

    public static OrderTimeoutClosePayload from(DomainEvent<?> event) {
        if (event == null) {
            throw new IllegalArgumentException("order.timeout.close 事件不能为空");
        }
        if (!(event.payload() instanceof Map<?, ?> rawPayload)) {
            throw new IllegalArgumentException("order.timeout.close 事件载荷格式非法");
        }
        Object value = rawPayload.get("orderNo");
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("order.timeout.close 缺少字段: orderNo");
        }
        return new OrderTimeoutClosePayload(String.valueOf(value));
    }
}
