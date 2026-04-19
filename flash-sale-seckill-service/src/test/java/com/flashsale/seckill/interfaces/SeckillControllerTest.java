package com.flashsale.seckill.interfaces;

import com.flashsale.common.mq.event.DomainEvent;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.seckill.FlashSaleSeckillApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FlashSaleSeckillApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeckillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private RedisScript<Long> seckillAttemptRedisScript;

    private HashOperations<String, Object, Object> hashOperations;

    @BeforeEach
    void setUp() {
        hashOperations = Mockito.mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void attemptReturnsProcessingAndPublishesOrderCreateEvent() throws Exception {
        Long activityId = 1001L;
        Long userId = 2001L;
        Map<Object, Object> detail = activityDetail(
                activityId,
                "2026-04-19T09:00:00",
                "2026-04-19T11:00:00",
                "PUBLISHED",
                "SINGLE",
                "1",
                "false",
                "THIRD_PARTY_IMPORTED"
        );
        when(stringRedisTemplate.opsForHash().entries(RedisKeys.activityDetail(activityId))).thenReturn(detail);
        when(stringRedisTemplate.execute(
                eq(seckillAttemptRedisScript),
                anyList(),
                Mockito.<Object>any(),
                Mockito.<Object>any(),
                Mockito.<Object>any(),
                Mockito.<Object>any(),
                Mockito.<Object>any()
        )).thenReturn(0L);

        mockMvc.perform(post("/api/seckill/activities/{activityId}/attempt", activityId)
                        .header("X-User-Id", String.valueOf(userId))
                        .header("X-Username", "buyer")
                        .header("X-Role", "USER")
                        .header("X-Request-Id", "REQ-SECKILL-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SECKILL_PROCESSING"))
                .andExpect(jsonPath("$.message").value("请求已受理，请稍后查询结果"))
                .andExpect(jsonPath("$.requestId").value("REQ-SECKILL-001"))
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        verify(hashOperations, times(1)).putAll(eq(RedisKeys.seckillResult(activityId, userId)), any());
        verify(stringRedisTemplate, times(1)).expire(eq(RedisKeys.seckillResult(activityId, userId)), any());

        ArgumentCaptor<DomainEvent<?>> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq("flash.sale.event.exchange"), eq("order.create"), eventCaptor.capture());

        DomainEvent<?> event = eventCaptor.getValue();
        assertThat(event.eventType()).isEqualTo("order.create");
        assertThat(event.bizKey()).isEqualTo("activity:1001:user:2001:req:REQ-SECKILL-001");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) event.payload();
        assertThat(payload).containsEntry("activityId", activityId)
                .containsEntry("userId", userId)
                .containsEntry("requestId", "REQ-SECKILL-001")
                .containsEntry("needPayment", false)
                .containsEntry("codeSourceMode", "THIRD_PARTY_IMPORTED");
    }

    @Test
    void attemptReturnsMappedBusinessFailureWhenLuaRejects() throws Exception {
        Long activityId = 1001L;
        Map<Object, Object> detail = activityDetail(
                activityId,
                "2026-04-19T09:00:00",
                "2026-04-19T11:00:00",
                "PUBLISHED",
                "SINGLE",
                "1",
                "false",
                "SYSTEM_GENERATED"
        );
        when(stringRedisTemplate.opsForHash().entries(RedisKeys.activityDetail(activityId))).thenReturn(detail);
        when(stringRedisTemplate.execute(
                eq(seckillAttemptRedisScript),
                anyList(),
                Mockito.<Object>any(),
                Mockito.<Object>any(),
                Mockito.<Object>any(),
                Mockito.<Object>any(),
                Mockito.<Object>any()
        )).thenReturn(3L);

        mockMvc.perform(post("/api/seckill/activities/{activityId}/attempt", activityId)
                        .header("X-User-Id", "2001")
                        .header("X-Username", "buyer")
                        .header("X-Role", "USER")
                        .header("X-Request-Id", "REQ-SECKILL-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.message").value("库存不足"))
                .andExpect(jsonPath("$.requestId").value("REQ-SECKILL-002"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(hashOperations, never()).putAll(eq(RedisKeys.seckillResult(activityId, 2001L)), any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), Mockito.any(Object.class));
    }

    @Test
    void attemptRejectsMissingRequestIdHeader() throws Exception {
        mockMvc.perform(post("/api/seckill/activities/{activityId}/attempt", 1001L)
                        .header("X-User-Id", "2001")
                        .header("X-Username", "buyer")
                        .header("X-Role", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("缺少 X-Request-Id 请求头"));
    }

    @Test
    void resultQueryReturnsCachedSeckillResult() throws Exception {
        Map<Object, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("orderNo", "SO202604190001");
        result.put("message", "抢购成功");
        result.put("code", "ABCDEF123456");
        result.put("updatedAt", "2026-04-19T10:05:00");
        when(stringRedisTemplate.opsForHash().entries(RedisKeys.seckillResult(1001L, 2001L))).thenReturn(result);

        mockMvc.perform(get("/api/seckill/results/{activityId}", 1001L)
                        .header("X-User-Id", "2001")
                        .header("X-Username", "buyer")
                        .header("X-Role", "USER")
                        .header("X-Request-Id", "REQ-SECKILL-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.requestId").value("REQ-SECKILL-003"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.orderNo").value("SO202604190001"))
                .andExpect(jsonPath("$.data.code").value("ABCDEF123456"));
    }

    @Test
    void resultQueryReturnsInitWhenNoCacheExists() throws Exception {
        when(stringRedisTemplate.opsForHash().entries(RedisKeys.seckillResult(1001L, 2001L))).thenReturn(Map.of());

        mockMvc.perform(get("/api/seckill/results/{activityId}", 1001L)
                        .header("X-User-Id", "2001")
                        .header("X-Username", "buyer")
                        .header("X-Role", "USER")
                        .header("X-Request-Id", "REQ-SECKILL-004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("INIT"))
                .andExpect(jsonPath("$.data.message").value("暂无抢购结果"));
    }

    private Map<Object, Object> activityDetail(
            Long activityId,
            String startTime,
            String endTime,
            String publishStatus,
            String purchaseLimitType,
            String purchaseLimitCount,
            String needPayment,
            String codeSourceMode
    ) {
        Map<Object, Object> detail = new LinkedHashMap<>();
        detail.put("id", String.valueOf(activityId));
        detail.put("startTime", startTime);
        detail.put("endTime", endTime);
        detail.put("publishStatus", publishStatus);
        detail.put("purchaseLimitType", purchaseLimitType);
        detail.put("purchaseLimitCount", purchaseLimitCount);
        detail.put("needPayment", needPayment);
        detail.put("codeSourceMode", codeSourceMode);
        return detail;
    }
}
