package com.flashsale.activity.application;

import com.flashsale.activity.FlashSaleActivityApplication;
import com.flashsale.activity.job.ActivityPublishScheduler;
import com.flashsale.common.redis.RedisKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = FlashSaleActivityApplication.class)
@ActiveProfiles("test")
class ActivityPublishSchedulerTest {

    @Autowired
    private ActivityPublishScheduler activityPublishScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private ZSetOperations<String, String> zSetOperations;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from redeem_code");
        jdbcTemplate.update("delete from activity_product");

        valueOperations = Mockito.mock(ValueOperations.class);
        hashOperations = Mockito.mock(HashOperations.class);
        zSetOperations = Mockito.mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void publishReadyActivitiesPublishesOnlyDueScheduledActivities() {
        Long dueActivityId = insertScheduledActivity("到点活动", LocalDateTime.now().minusMinutes(1));
        Long futureActivityId = insertScheduledActivity("未来活动", LocalDateTime.now().plusMinutes(20));

        activityPublishScheduler.publishReadyActivities();

        assertThat(queryPublishStatus(dueActivityId)).isEqualTo("PUBLISHED");
        assertThat(queryPublishStatus(futureActivityId)).isEqualTo("UNPUBLISHED");
        verify(valueOperations, times(1))
                .set(eq(RedisKeys.seckillStock(dueActivityId)), eq("15"), any());
        verify(hashOperations, times(1))
                .putAll(eq(RedisKeys.activityDetail(dueActivityId)), anyMap());
        verify(zSetOperations, times(1))
                .add(eq(RedisKeys.activityVisibleList()), eq(String.valueOf(dueActivityId)), anyDouble());
        verify(valueOperations, never())
                .set(eq(RedisKeys.seckillStock(futureActivityId)), any(), any());
    }

    private Long insertScheduledActivity(String title, LocalDateTime publishTime) {
        jdbcTemplate.update("""
                        insert into activity_product (
                          title, description, cover_url, total_stock, available_stock, price_amount, need_payment,
                          purchase_limit_type, purchase_limit_count, code_source_mode, publish_mode, publish_status,
                          publish_time, start_time, end_time, version, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                        """,
                title,
                title + "描述",
                "https://example.com/" + title + ".png",
                15,
                15,
                BigDecimal.ZERO,
                0,
                "SINGLE",
                1,
                "SYSTEM_GENERATED",
                "SCHEDULED",
                "UNPUBLISHED",
                publishTime,
                LocalDateTime.now().plusMinutes(30),
                LocalDateTime.now().plusMinutes(90)
        );
        return jdbcTemplate.queryForObject("select max(id) from activity_product", Long.class);
    }

    private String queryPublishStatus(Long activityId) {
        return jdbcTemplate.queryForObject(
                "select publish_status from activity_product where id = ?",
                String.class,
                activityId
        );
    }
}
