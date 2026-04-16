package com.flashsale.activity.application;

import com.flashsale.activity.domain.ActivityEntity;
import com.flashsale.activity.service.ActivityCacheService;
import com.flashsale.common.redis.RedisKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private ActivityCacheService activityCacheService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        activityCacheService = new ActivityCacheService(stringRedisTemplate);
    }

    @Test
    void warmUpWritesStockDetailAndVisibleIndex() {
        ActivityEntity activity = activity(1L, "PUBLISHED");

        activityCacheService.warmUp(activity);

        verify(valueOperations).set(eq(RedisKeys.seckillStock(1L)), eq("20"), any());
        ArgumentCaptor<Map<String, Object>> detailCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq(RedisKeys.activityDetail(1L)), detailCaptor.capture());
        verify(stringRedisTemplate).expire(eq(RedisKeys.activityDetail(1L)), any());
        verify(zSetOperations).add(eq(RedisKeys.activityVisibleList()), eq("1"), anyDouble());

        Map<String, Object> detail = detailCaptor.getValue();
        assertThat(detail.get("title")).isEqualTo("缓存预热活动");
        assertThat(detail.get("publishStatus")).isEqualTo("PUBLISHED");
        assertThat(detail.get("purchaseLimitCount")).isEqualTo("1");
    }

    @Test
    void clearRemovesStockDetailAndVisibleIndex() {
        ActivityEntity activity = activity(2L, "OFFLINE");

        activityCacheService.clear(activity);

        verify(stringRedisTemplate).delete(RedisKeys.seckillStock(2L));
        verify(stringRedisTemplate).delete(RedisKeys.activityDetail(2L));
        verify(zSetOperations).remove(RedisKeys.activityVisibleList(), "2");
    }

    private ActivityEntity activity(Long id, String publishStatus) {
        ActivityEntity activity = new ActivityEntity();
        activity.setId(id);
        activity.setTitle("缓存预热活动");
        activity.setDescription("缓存预热描述");
        activity.setCoverUrl("https://example.com/cache.png");
        activity.setTotalStock(20);
        activity.setAvailableStock(20);
        activity.setPriceAmount(BigDecimal.ZERO);
        activity.setNeedPayment(false);
        activity.setPurchaseLimitType("SINGLE");
        activity.setPurchaseLimitCount(1);
        activity.setCodeSourceMode("SYSTEM_GENERATED");
        activity.setPublishMode("IMMEDIATE");
        activity.setPublishStatus(publishStatus);
        activity.setPublishTime(LocalDateTime.now().minusMinutes(1));
        activity.setStartTime(LocalDateTime.now().plusMinutes(10));
        activity.setEndTime(LocalDateTime.now().plusHours(2));
        return activity;
    }
}
