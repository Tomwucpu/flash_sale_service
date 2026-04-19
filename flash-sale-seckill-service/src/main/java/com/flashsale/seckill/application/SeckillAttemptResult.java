package com.flashsale.seckill.application;

public record SeckillAttemptResult(
        String code,
        String message,
        SeckillAttemptResponse data
) {

    public static SeckillAttemptResult processing(Long activityId) {
        return new SeckillAttemptResult(
                "SECKILL_PROCESSING",
                "请求已受理，请稍后查询结果",
                new SeckillAttemptResponse(activityId, "PROCESSING")
        );
    }

    public static SeckillAttemptResult failure(String code, String message) {
        return new SeckillAttemptResult(code, message, null);
    }
}
