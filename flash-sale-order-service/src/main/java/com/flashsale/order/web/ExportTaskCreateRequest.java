package com.flashsale.order.web;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ExportTaskCreateRequest(
        @NotNull(message = "activityId 不能为空") Long activityId,
        @NotNull(message = "format 不能为空") String format,
        Map<String, Object> filters
) {
}
