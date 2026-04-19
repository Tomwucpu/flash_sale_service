package com.flashsale.order.application;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DefaultOrderNoGenerator implements OrderNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Clock clock;

    private final AtomicInteger sequence = new AtomicInteger();

    public DefaultOrderNoGenerator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String nextOrderNo() {
        int current = sequence.updateAndGet(value -> value >= 9999 ? 1 : value + 1);
        return "SO" + LocalDateTime.now(clock).format(FORMATTER) + "%04d".formatted(current);
    }
}
