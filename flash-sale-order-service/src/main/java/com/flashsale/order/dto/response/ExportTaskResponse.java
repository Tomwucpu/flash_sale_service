package com.flashsale.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportTaskResponse(
        Long id,
        Long activityId,
        Long operatorId,
        String format,
        Map<String, Object> filters,
        String status,
        String fileUrl,
        String failReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
