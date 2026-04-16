package com.flashsale.common.redis;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String seckillStock(Long activityId) {
        return "seckill:stock:" + activityId;
    }

    public static String seckillLimit(Long activityId, Long userId) {
        return "seckill:limit:" + activityId + ":" + userId;
    }

    public static String seckillRequest(Long activityId, Long userId, String requestId) {
        return "seckill:req:" + activityId + ":" + userId + ":" + requestId;
    }

    public static String seckillResult(Long activityId, Long userId) {
        return "seckill:result:" + activityId + ":" + userId;
    }

    public static String activityDetail(Long activityId) {
        return "activity:detail:" + activityId;
    }

    public static String activityVisibleList() {
        return "activity:visible:list";
    }
}
