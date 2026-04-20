package com.flashsale.order.application;

import com.flashsale.common.mq.event.DomainEvent;

import java.util.Map;

public record ExportGeneratePayload(Long taskId) {

    public static ExportGeneratePayload from(DomainEvent<?> event) {
        if (event == null) {
            throw new IllegalArgumentException("export.generate 事件不能为空");
        }
        if (!(event.payload() instanceof Map<?, ?> rawPayload)) {
            throw new IllegalArgumentException("export.generate 事件载荷格式非法");
        }
        Object value = rawPayload.get("taskId");
        if (value == null) {
            throw new IllegalArgumentException("export.generate 缺少字段: taskId");
        }
        if (value instanceof Number number) {
            return new ExportGeneratePayload(number.longValue());
        }
        return new ExportGeneratePayload(Long.parseLong(String.valueOf(value)));
    }
}
