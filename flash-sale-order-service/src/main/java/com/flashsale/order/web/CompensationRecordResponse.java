package com.flashsale.order.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompensationRecordResponse(
        Long id,
        String bizType,
        String bizKey,
        String sourceEvent,
        String status,
        String reason,
        String resolutionNote,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
}
