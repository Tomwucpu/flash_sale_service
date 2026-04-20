package com.flashsale.order.application;

import com.flashsale.common.mq.event.DomainEvent;

import java.util.Map;

public record ExportDeadLetterPayload(Long taskId, String bizKey, String reason) {

    public static ExportDeadLetterPayload from(DomainEvent<?> event) {
        if (event == null) {
            throw new IllegalArgumentException("export.generate.dead 事件不能为空");
        }
        if (!(event.payload() instanceof Map<?, ?> rawPayload)) {
            throw new IllegalArgumentException("export.generate.dead 事件载荷格式非法");
        }
        Object taskIdValue = rawPayload.get("taskId");
        Object reasonValue = rawPayload.get("reason");
        if (taskIdValue == null) {
            throw new IllegalArgumentException("export.generate.dead 缺少字段: taskId");
        }
        if (reasonValue == null || String.valueOf(reasonValue).isBlank()) {
            throw new IllegalArgumentException("export.generate.dead 缺少字段: reason");
        }
        Long taskId = taskIdValue instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(taskIdValue));
        return new ExportDeadLetterPayload(taskId, event.bizKey(), String.valueOf(reasonValue));
    }
}
