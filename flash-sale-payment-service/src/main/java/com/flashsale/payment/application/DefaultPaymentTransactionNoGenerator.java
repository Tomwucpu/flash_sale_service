package com.flashsale.payment.application;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DefaultPaymentTransactionNoGenerator implements PaymentTransactionNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Clock clock;

    private final AtomicInteger sequence = new AtomicInteger();

    public DefaultPaymentTransactionNoGenerator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String nextTransactionNo() {
        int current = sequence.updateAndGet(value -> value >= 9999 ? 1 : value + 1);
        return "TXN" + LocalDateTime.now(clock).format(FORMATTER) + "%04d".formatted(current);
    }
}
