package com.flashsale.seckill.application;

import com.flashsale.common.mq.event.DomainEvent;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.common.security.context.UserContext;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SeckillService {

    private static final Duration REQUEST_MARKER_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final RedisScript<Long> seckillAttemptRedisScript;
    private final Clock clock;
    private final String eventExchange;

    public SeckillService(
            StringRedisTemplate stringRedisTemplate,
            RabbitTemplate rabbitTemplate,
            RedisScript<Long> seckillAttemptRedisScript,
            Clock clock,
            @Value("${flash-sale.mq.exchange:flash.sale.event.exchange}") String eventExchange
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.seckillAttemptRedisScript = seckillAttemptRedisScript;
        this.clock = clock;
        this.eventExchange = eventExchange;
    }

    public SeckillAttemptResult attempt(Long activityId, String requestId, UserContext userContext) {
        ActivitySnapshot activity = loadActivitySnapshot(activityId);
        if (!activity.published()) {
            return SeckillAttemptResult.failure("ACTIVITY_OFFLINE", "活动已下线或未发布");
        }

        Long luaResult = stringRedisTemplate.execute(
                seckillAttemptRedisScript,
                List.of(
                        RedisKeys.seckillStock(activityId),
                        RedisKeys.seckillLimit(activityId, userContext.userId()),
                        RedisKeys.seckillRequest(activityId, userContext.userId(), requestId)
                ),
                String.valueOf(Instant.now(clock).toEpochMilli()),
                String.valueOf(activity.startEpochMillis()),
                String.valueOf(activity.endEpochMillis()),
                String.valueOf(activity.purchaseLimitCount()),
                String.valueOf(REQUEST_MARKER_TTL.toSeconds())
        );

        LuaResultCode resultCode = LuaResultCode.from(luaResult);
        if (resultCode != LuaResultCode.SUCCESS) {
            return SeckillAttemptResult.failure(resultCode.apiCode, resultCode.message);
        }

        rabbitTemplate.convertAndSend(
                eventExchange,
                "order.create",
                DomainEvent.create(
                        "order.create",
                        "activity:%d:user:%d:req:%s".formatted(activityId, userContext.userId(), requestId),
                        orderCreatePayload(activityId, userContext.userId(), requestId, activity),
                        clock
                )
        );

        writeProcessingResult(activityId, userContext.userId(), activity);
        return SeckillAttemptResult.processing(activityId);
    }

    public SeckillResultResponse queryResult(Long activityId, UserContext userContext) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(RedisKeys.seckillResult(activityId, userContext.userId()));
        if (entries == null || entries.isEmpty()) {
            return new SeckillResultResponse("INIT", null, "暂无抢购结果", null, null);
        }
        return new SeckillResultResponse(
                value(entries, "status"),
                blankToNull(value(entries, "orderNo")),
                blankToNull(value(entries, "message")),
                blankToNull(value(entries, "code")),
                blankToNull(value(entries, "updatedAt"))
        );
    }

    private ActivitySnapshot loadActivitySnapshot(Long activityId) {
        Map<Object, Object> detail = stringRedisTemplate.opsForHash().entries(RedisKeys.activityDetail(activityId));
        if (detail == null || detail.isEmpty()) {
            return ActivitySnapshot.offline();
        }
        return ActivitySnapshot.from(detail);
    }

    private Map<String, Object> orderCreatePayload(
            Long activityId,
            Long userId,
            String requestId,
            ActivitySnapshot activity
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activityId", activityId);
        payload.put("userId", userId);
        payload.put("requestId", requestId);
        payload.put("needPayment", activity.needPayment());
        payload.put("codeSourceMode", activity.codeSourceMode());
        return payload;
    }

    private void writeProcessingResult(Long activityId, Long userId, ActivitySnapshot activity) {
        String resultKey = RedisKeys.seckillResult(activityId, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "PROCESSING");
        result.put("message", "请求已受理，请稍后查询结果");
        result.put("updatedAt", LocalDateTime.now(clock).toString());
        stringRedisTemplate.opsForHash().putAll(resultKey, result);
        stringRedisTemplate.expire(resultKey, ttl(activity.endTime()));
    }

    private Duration ttl(LocalDateTime endTime) {
        Duration ttl = Duration.between(LocalDateTime.now(clock), endTime.plusHours(24));
        if (ttl.isNegative() || ttl.isZero()) {
            return Duration.ofHours(1);
        }
        return ttl;
    }

    private String value(Map<Object, Object> source, String key) {
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private enum LuaResultCode {
        SUCCESS(0L, "SECKILL_PROCESSING", "请求已受理，请稍后查询结果"),
        ACTIVITY_NOT_STARTED(1L, "ACTIVITY_NOT_STARTED", "活动未开始"),
        ACTIVITY_ENDED(2L, "ACTIVITY_ENDED", "活动已结束"),
        OUT_OF_STOCK(3L, "OUT_OF_STOCK", "库存不足"),
        DUPLICATE_REQUEST(4L, "DUPLICATE_REQUEST", "重复请求"),
        OVER_PURCHASE_LIMIT(5L, "OVER_PURCHASE_LIMIT", "超出限购");

        private final Long luaCode;
        private final String apiCode;
        private final String message;

        LuaResultCode(Long luaCode, String apiCode, String message) {
            this.luaCode = luaCode;
            this.apiCode = apiCode;
            this.message = message;
        }

        private static LuaResultCode from(Long luaCode) {
            if (luaCode == null) {
                throw new IllegalStateException("秒杀脚本执行失败");
            }
            for (LuaResultCode candidate : values()) {
                if (candidate.luaCode.equals(luaCode)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("未知秒杀脚本返回码: " + luaCode);
        }
    }

    private record ActivitySnapshot(
            boolean published,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int purchaseLimitCount,
            boolean needPayment,
            String codeSourceMode
    ) {

        private static ActivitySnapshot offline() {
            LocalDateTime now = LocalDateTime.now();
            return new ActivitySnapshot(false, now, now, 1, false, "SYSTEM_GENERATED");
        }

        private static ActivitySnapshot from(Map<Object, Object> detail) {
            String publishStatus = valueOf(detail, "publishStatus");
            LocalDateTime startTime = parseDateTime(valueOf(detail, "startTime"));
            LocalDateTime endTime = parseDateTime(valueOf(detail, "endTime"));
            String purchaseLimitType = valueOf(detail, "purchaseLimitType");
            int purchaseLimitCount = parsePurchaseLimit(purchaseLimitType, valueOf(detail, "purchaseLimitCount"));
            boolean needPayment = Boolean.parseBoolean(valueOf(detail, "needPayment"));
            String codeSourceMode = valueOf(detail, "codeSourceMode");
            return new ActivitySnapshot(
                    "PUBLISHED".equalsIgnoreCase(publishStatus),
                    startTime,
                    endTime,
                    purchaseLimitCount,
                    needPayment,
                    codeSourceMode == null || codeSourceMode.isBlank() ? "SYSTEM_GENERATED" : codeSourceMode
            );
        }

        private long startEpochMillis() {
            return startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        private long endEpochMillis() {
            return endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        private static String valueOf(Map<Object, Object> source, String key) {
            Object value = source.get(key);
            return value == null ? null : String.valueOf(value);
        }

        private static LocalDateTime parseDateTime(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("活动缓存缺少时间字段");
            }
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("活动缓存时间格式非法: " + value, exception);
            }
        }

        private static int parsePurchaseLimit(String purchaseLimitType, String purchaseLimitCount) {
            if ("SINGLE".equalsIgnoreCase(purchaseLimitType)) {
                return 1;
            }
            try {
                int limit = Integer.parseInt(purchaseLimitCount);
                return Math.max(limit, 1);
            } catch (NumberFormatException exception) {
                return 1;
            }
        }
    }
}
