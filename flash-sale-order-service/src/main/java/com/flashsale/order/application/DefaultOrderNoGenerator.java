package com.flashsale.order.application;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认订单号生成器
 * 格式: SO + yyyyMMddHHmmss(14位) + 自增序列(4位)
 */
@Component
public class DefaultOrderNoGenerator implements OrderNoGenerator {

    // 时间格式化(精确到秒)
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Clock clock;

    // 原子整数，保证多线程并发下序列号生成的线程安全
    private final AtomicInteger sequence = new AtomicInteger();

    public DefaultOrderNoGenerator(Clock clock) {
        this.clock = clock;
    }

    /**
     * 生成下一个唯一的订单号
     * 单节点支持最大 9999/秒 的订单生成速率
     */
    @Override
    public String nextOrderNo() {
        // 自增序列，超过 9999 时重置回 1
        int current = sequence.updateAndGet(value -> value >= 9999 ? 1 : value + 1);
        return "SO" + LocalDateTime.now(clock).format(FORMATTER) + "%04d".formatted(current);
    }
}
