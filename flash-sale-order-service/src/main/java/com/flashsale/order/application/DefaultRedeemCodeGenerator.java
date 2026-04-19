package com.flashsale.order.application;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class DefaultRedeemCodeGenerator implements RedeemCodeGenerator {

    private final Clock clock;

    public DefaultRedeemCodeGenerator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String nextCode() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "RC" + Instant.now(clock).toEpochMilli() + suffix;
    }
}
