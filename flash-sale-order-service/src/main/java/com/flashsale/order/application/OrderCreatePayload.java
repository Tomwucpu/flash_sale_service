package com.flashsale.order.application;

import com.flashsale.common.mq.event.DomainEvent;

import java.util.Map;

public record OrderCreatePayload(
        Long activityId,
        Long userId,
        String requestId,
        boolean needPayment,
        String codeSourceMode
) {

    public static OrderCreatePayload from(DomainEvent<?> event) {
        if (event == null) {
            throw new IllegalArgumentException("order.create 事件不能为空");
        }
        if (!(event.payload() instanceof Map<?, ?> rawPayload)) {
            throw new IllegalArgumentException("order.create 事件载荷格式非法");
        }
        Long activityId = longValue(rawPayload.get("activityId"), "activityId");
        Long userId = longValue(rawPayload.get("userId"), "userId");
        String requestId = stringValue(rawPayload.get("requestId"), "requestId");
        boolean needPayment = booleanValue(rawPayload.get("needPayment"));
        String codeSourceMode = stringValue(rawPayload.get("codeSourceMode"), "codeSourceMode");
        return new OrderCreatePayload(activityId, userId, requestId, needPayment, codeSourceMode);
    }

    public String purchaseUniqueKey() {
        return "activity:%d:user:%d:req:%s".formatted(activityId, userId, requestId);
    }

    private static Long longValue(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("order.create 缺少字段: " + fieldName);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static String stringValue(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("order.create 缺少字段: " + fieldName);
        }
        String actual = String.valueOf(value);
        if (actual.isBlank()) {
            throw new IllegalArgumentException("order.create 字段为空: " + fieldName);
        }
        return actual;
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
