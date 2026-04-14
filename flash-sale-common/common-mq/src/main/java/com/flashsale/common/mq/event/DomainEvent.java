package com.flashsale.common.mq.event;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

public record DomainEvent<T>(
        String messageId,
        String eventType,
        String bizKey,
        LocalDateTime occurTime,
        T payload
) {

    public static <T> DomainEvent<T> create(String eventType, String bizKey, T payload, Clock clock) {
        return new DomainEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                bizKey,
                LocalDateTime.now(clock),
                payload
        );
    }
}
