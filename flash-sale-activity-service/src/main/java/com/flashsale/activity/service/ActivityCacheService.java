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

/**
 * 活动缓存服务。
 * <p>
 * 负责活动发布后在 Redis 预热库存、详情以及可见活动列表，并在下线/删除时清理缓存。
 */
@Service
public class ActivityCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    public ActivityCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 预热活动缓存数据。
     */
    public void warmUp(ActivityEntity activity) {
        Duration ttl = ttl(activity.getEndTime());
        // 缓存库存计数（用于高并发扣减）
        stringRedisTemplate.opsForValue().set(
                RedisKeys.seckillStock(activity.getId()),
                String.valueOf(activity.getAvailableStock()),
                ttl
        );

        // 缓存活动详情（Hash 结构）
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

        // 将活动加入可见列表，score 使用发布时间时间戳
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

    /**
     * 清理活动相关缓存数据。
     */
    public void clear(ActivityEntity activity) {
        stringRedisTemplate.delete(RedisKeys.seckillStock(activity.getId()));
        stringRedisTemplate.delete(RedisKeys.activityDetail(activity.getId()));
        stringRedisTemplate.opsForZSet().remove(
                RedisKeys.activityVisibleList(),
                String.valueOf(activity.getId())
        );
    }

    /**
     * 计算缓存过期时间。
     * <p>
     * 默认使用“活动结束时间 + 24 小时”，若已过期则兜底 1 小时。
     */
    private Duration ttl(LocalDateTime endTime) {
        Duration ttl = Duration.between(LocalDateTime.now(), endTime.plusHours(24));
        if (ttl.isNegative() || ttl.isZero()) {
            return Duration.ofHours(1);
        }
        return ttl;
    }

    /**
     * 将可空字符串转换为非空字符串，避免写入 Redis 空值。
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}
