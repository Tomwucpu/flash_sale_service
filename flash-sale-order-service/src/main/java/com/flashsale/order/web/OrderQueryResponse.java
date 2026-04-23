package com.flashsale.order.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderQueryResponse(
        String orderNo,
        Long activityId,
        Long userId,
        String orderStatus,
        String payStatus,
        String codeStatus,
        BigDecimal priceAmount,
        String failReason,
        String code,
        LocalDateTime updatedAt
) {
}
