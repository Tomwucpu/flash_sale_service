package com.flashsale.order.web;

import java.time.LocalDateTime;

public record OrderCodeResponse(
        String orderNo,
        Long activityId,
        String orderStatus,
        String payStatus,
        String codeStatus,
        String code,
        LocalDateTime updatedAt
) {
}
