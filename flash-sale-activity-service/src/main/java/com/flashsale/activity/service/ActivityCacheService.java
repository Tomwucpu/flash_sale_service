package com.flashsale.activity.service;

import com.flashsale.activity.domain.ActivityEntity;
import com.flashsale.common.redis.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ActivityCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    public ActivityCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void warmUp(ActivityEntity activity) {
        Duration ttl = ttl(activity.getEndTime());
        stringRedisTemplate.opsForValue().set(
                RedisKeys.seckillStock(activity.getId()),
                String.valueOf(activity.getAvailableStock()),
                ttl
        );

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", String.valueOf(activity.getId()));
        detail.put("title", safe(activity.getTitle()));
        detail.put("description", safe(activity.getDescription()));
        detail.put("coverUrl", safe(activity.getCoverUrl()));
        detail.put("totalStock", String.valueOf(activity.getTotalStock()));
        detail.put("availableStock", String.valueOf(activity.getAvailableStock()));
        detail.put("priceAmount", activity.getPriceAmount().toPlainString());
        detail.put("needPayment", String.valueOf(activity.getNeedPayment()));
        detail.put("purchaseLimitType", safe(activity.getPurchaseLimitType()));
        detail.put("purchaseLimitCount", String.valueOf(activity.getPurchaseLimitCount()));
        detail.put("codeSourceMode", safe(activity.getCodeSourceMode()));
        detail.put("publishMode", safe(activity.getPublishMode()));
        detail.put("publishStatus", safe(activity.getPublishStatus()));
        detail.put("publishTime", String.valueOf(activity.getPublishTime()));
        detail.put("startTime", String.valueOf(activity.getStartTime()));
        detail.put("endTime", String.valueOf(activity.getEndTime()));
        stringRedisTemplate.opsForHash().putAll(RedisKeys.activityDetail(activity.getId()), detail);
        stringRedisTemplate.expire(RedisKeys.activityDetail(activity.getId()), ttl);

        double score = activity.getPublishTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        stringRedisTemplate.opsForZSet().add(
                RedisKeys.activityVisibleList(),
                String.valueOf(activity.getId()),
                score
        );
    }

    public void clear(ActivityEntity activity) {
        stringRedisTemplate.delete(RedisKeys.seckillStock(activity.getId()));
        stringRedisTemplate.delete(RedisKeys.activityDetail(activity.getId()));
        stringRedisTemplate.opsForZSet().remove(
                RedisKeys.activityVisibleList(),
                String.valueOf(activity.getId())
        );
    }

    private Duration ttl(LocalDateTime endTime) {
        Duration ttl = Duration.between(LocalDateTime.now(), endTime.plusHours(24));
        if (ttl.isNegative() || ttl.isZero()) {
            return Duration.ofHours(1);
        }
        return ttl;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
