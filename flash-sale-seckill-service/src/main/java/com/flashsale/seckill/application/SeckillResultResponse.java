package com.flashsale.seckill.application;

public record SeckillResultResponse(
        String status,
        String orderNo,
        String message,
        String code,
        String updatedAt
) {
}
