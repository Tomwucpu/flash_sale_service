package com.flashsale.common.mq.event;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DomainEventTest {

    @Test
    void createUsesClockAndCopiesBusinessFields() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-14T10:00:00Z"), ZoneId.of("Asia/Shanghai"));
        DomainEvent<Map<String, Object>> event = DomainEvent.create(
                "order.create",
                "activity:1001:user:2001:req:REQ001",
                Map.of("activityId", 1001L),
                fixedClock
        );

        assertNotNull(event.messageId());
        assertEquals("order.create", event.eventType());
        assertEquals("activity:1001:user:2001:req:REQ001", event.bizKey());
        assertEquals(LocalDateTime.of(2026, 4, 14, 18, 0), event.occurTime());
        assertEquals(Map.of("activityId", 1001L), event.payload());
    }
}
