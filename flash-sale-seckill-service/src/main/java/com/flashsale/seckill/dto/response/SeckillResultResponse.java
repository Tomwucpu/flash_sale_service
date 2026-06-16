package com.flashsale.seckill.dto.response;

public record SeckillResultResponse(
        String status,
        String orderNo,
        String message,
        String code,
        String updatedAt
) {
}
