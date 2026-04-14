package com.flashsale.common.redis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisKeysTest {

    @Test
    void buildsSeckillKeysWithDocumentedPattern() {
        assertEquals("seckill:stock:1001", RedisKeys.seckillStock(1001L));
        assertEquals("seckill:limit:1001:2001", RedisKeys.seckillLimit(1001L, 2001L));
        assertEquals("seckill:req:1001:2001:REQ001", RedisKeys.seckillRequest(1001L, 2001L, "REQ001"));
        assertEquals("seckill:result:1001:2001", RedisKeys.seckillResult(1001L, 2001L));
        assertEquals("activity:detail:1001", RedisKeys.activityDetail(1001L));
    }
}
