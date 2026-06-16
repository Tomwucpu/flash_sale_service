package com.flashsale.order.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 默认兑换码生成器
 * 格式: RC + 毫秒级时间戳 + 8位随机大写字符
 */
@Component
public class DefaultRedeemCodeGenerator implements RedeemCodeGenerator {

    private final Clock clock;

    public DefaultRedeemCodeGenerator(Clock clock) {
        this.clock = clock;
    }

    /**
     * 生成下一个唯一的兑换码
     */
    @Override
    public String nextCode() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "RC" + Instant.now(clock).toEpochMilli() + suffix;
    }
}
