package com.flashsale.order.application;

import com.flashsale.common.mq.event.DomainEvent;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.order.FlashSaleOrderApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = FlashSaleOrderApplication.class)
@ActiveProfiles("test")
class OrderCreateConsumerTest {

    @Autowired
    private OrderCreateConsumer orderCreateConsumer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private OrderNoGenerator orderNoGenerator;

    @MockBean
    private RedeemCodeGenerator redeemCodeGenerator;

    @Autowired
    private Clock clock;

    private HashOperations<String, Object, Object> hashOperations;

    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from redeem_code");
        jdbcTemplate.update("delete from order_record");
        jdbcTemplate.update("delete from activity_product");

        hashOperations = Mockito.mock(HashOperations.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.expire(anyString(), any())).thenReturn(true);
    }

    @Test
    void importedCodeFreeOrderSucceedsAndWritesRedisResult() {
        Long activityId = insertActivity("THIRD_PARTY_IMPORTED");
        insertImportedCode(activityId, "IMPORTED-CODE-001");
        when(orderNoGenerator.nextOrderNo()).thenReturn("SO202604190001");

        orderCreateConsumer.onOrderCreate(orderCreateEvent(activityId, 2001L, "REQ-ORDER-001", false, "THIRD_PARTY_IMPORTED"));

        Map<String, Object> order = jdbcTemplate.queryForMap("select * from order_record where order_no = ?", "SO202604190001");
        assertThat(order)
                .containsEntry("activity_id", activityId)
                .containsEntry("user_id", 2001L)
                .containsEntry("request_id", "REQ-ORDER-001")
                .containsEntry("purchase_unique_key", "activity:%d:user:%d:req:%s".formatted(activityId, 2001L, "REQ-ORDER-001"))
                .containsEntry("order_status", "CONFIRMED")
                .containsEntry("pay_status", "NO_NEED")
                .containsEntry("code_status", "ISSUED")
                .containsEntry("fail_reason", null);

        Map<String, Object> code = jdbcTemplate.queryForMap("select * from redeem_code where code = ?", "IMPORTED-CODE-001");
        assertThat(code)
                .containsEntry("status", "ASSIGNED")
                .containsEntry("assigned_user_id", 2001L)
                .containsEntry("assigned_order_id", order.get("id"));

        ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq(RedisKeys.seckillResult(activityId, 2001L)), resultCaptor.capture());
        verify(stringRedisTemplate).expire(eq(RedisKeys.seckillResult(activityId, 2001L)), any());
        assertThat(resultCaptor.getValue())
                .containsEntry("status", "SUCCESS")
                .containsEntry("orderNo", "SO202604190001")
                .containsEntry("message", "抢购成功")
                .containsEntry("code", "IMPORTED-CODE-001");
        verify(valueOperations, never()).increment(anyString());
        verify(valueOperations, never()).decrement(anyString());
    }

    @Test
    void systemGeneratedFreeOrderSucceedsAndPersistsGeneratedCode() {
        Long activityId = insertActivity("SYSTEM_GENERATED");
        when(orderNoGenerator.nextOrderNo()).thenReturn("SO202604190002");
        when(redeemCodeGenerator.nextCode()).thenReturn("SYSTEM-CODE-001");

        orderCreateConsumer.onOrderCreate(orderCreateEvent(activityId, 2002L, "REQ-ORDER-002", false, "SYSTEM_GENERATED"));

        Map<String, Object> order = jdbcTemplate.queryForMap("select * from order_record where order_no = ?", "SO202604190002");
        assertThat(order)
                .containsEntry("order_status", "CONFIRMED")
                .containsEntry("code_status", "ISSUED");

        Map<String, Object> code = jdbcTemplate.queryForMap("select * from redeem_code where code = ?", "SYSTEM-CODE-001");
        assertThat(code)
                .containsEntry("activity_id", activityId)
                .containsEntry("source_type", "SYSTEM_GENERATED")
                .containsEntry("status", "ASSIGNED")
                .containsEntry("assigned_user_id", 2002L)
                .containsEntry("assigned_order_id", order.get("id"));
    }

    @Test
    void duplicateDeliveryDoesNotCreateSecondOrderOrSecondCode() {
        Long activityId = insertActivity("SYSTEM_GENERATED");
        when(orderNoGenerator.nextOrderNo()).thenReturn("SO202604190003");
        when(redeemCodeGenerator.nextCode()).thenReturn("SYSTEM-CODE-002");

        DomainEvent<Map<String, Object>> event = orderCreateEvent(activityId, 2003L, "REQ-ORDER-003", false, "SYSTEM_GENERATED");
        orderCreateConsumer.onOrderCreate(event);
        orderCreateConsumer.onOrderCreate(event);

        assertThat(jdbcTemplate.queryForObject("select count(1) from order_record", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("select count(1) from redeem_code where source_type = 'SYSTEM_GENERATED'", Long.class))
                .isEqualTo(1L);
    }

    @Test
    void importedCodeShortageMarksOrderFailedAndCompensatesRedis() {
        Long activityId = insertActivity("THIRD_PARTY_IMPORTED");
        when(orderNoGenerator.nextOrderNo()).thenReturn("SO202604190004");
        when(valueOperations.decrement(RedisKeys.seckillLimit(activityId, 2004L))).thenReturn(0L);

        orderCreateConsumer.onOrderCreate(orderCreateEvent(activityId, 2004L, "REQ-ORDER-004", false, "THIRD_PARTY_IMPORTED"));

        Map<String, Object> order = jdbcTemplate.queryForMap("select * from order_record where order_no = ?", "SO202604190004");
        assertThat(order)
                .containsEntry("order_status", "FAILED")
                .containsEntry("code_status", "PENDING")
                .containsEntry("fail_reason", "IMPORTED_CODE_UNAVAILABLE");

        ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq(RedisKeys.seckillResult(activityId, 2004L)), resultCaptor.capture());
        assertThat(resultCaptor.getValue())
                .containsEntry("status", "FAIL")
                .containsEntry("orderNo", "SO202604190004")
                .containsEntry("message", "兑换码不足")
                .containsEntry("code", "");
        verify(valueOperations).increment(RedisKeys.seckillStock(activityId));
        verify(valueOperations).decrement(RedisKeys.seckillLimit(activityId, 2004L));
    }

    @Test
    void unexpectedOrderInsertFailureWritesFailureAndCompensatesRedis() {
        Long activityId = insertActivity("SYSTEM_GENERATED");
        insertOrder(
                "SO202604190005",
                activityId,
                9001L,
                "REQ-EXISTING-001",
                "activity:%d:user:%d:req:%s".formatted(activityId, 9001L, "REQ-EXISTING-001"),
                "CONFIRMED",
                "NO_NEED",
                "ISSUED",
                null
        );
        when(orderNoGenerator.nextOrderNo()).thenReturn("SO202604190005");
        when(valueOperations.decrement(RedisKeys.seckillLimit(activityId, 2005L))).thenReturn(0L);

        orderCreateConsumer.onOrderCreate(orderCreateEvent(activityId, 2005L, "REQ-ORDER-005", false, "SYSTEM_GENERATED"));

        assertThat(jdbcTemplate.queryForObject(
                "select count(1) from order_record where purchase_unique_key = ?",
                Long.class,
                "activity:%d:user:%d:req:%s".formatted(activityId, 2005L, "REQ-ORDER-005")
        )).isZero();

        ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq(RedisKeys.seckillResult(activityId, 2005L)), resultCaptor.capture());
        assertThat(resultCaptor.getValue())
                .containsEntry("status", "FAIL")
                .containsEntry("orderNo", "")
                .containsEntry("message", "订单处理失败")
                .containsEntry("code", "");
        verify(valueOperations).increment(RedisKeys.seckillStock(activityId));
        verify(valueOperations).decrement(RedisKeys.seckillLimit(activityId, 2005L));
    }

    private DomainEvent<Map<String, Object>> orderCreateEvent(
            Long activityId,
            Long userId,
            String requestId,
            boolean needPayment,
            String codeSourceMode
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activityId", activityId);
        payload.put("userId", userId);
        payload.put("requestId", requestId);
        payload.put("needPayment", needPayment);
        payload.put("codeSourceMode", codeSourceMode);
        return DomainEvent.create(
                "order.create",
                "activity:%d:user:%d:req:%s".formatted(activityId, userId, requestId),
                payload,
                clock
        );
    }

    private Long insertActivity(String codeSourceMode) {
        jdbcTemplate.update("""
                        insert into activity_product (
                          title, description, cover_url, total_stock, available_stock, price_amount, need_payment,
                          purchase_limit_type, purchase_limit_count, code_source_mode, publish_mode, publish_status,
                          publish_time, start_time, end_time, version, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, DATEADD('HOUR', 2, CURRENT_TIMESTAMP), 0, 0)
                        """,
                "Task6活动",
                "Task6活动描述",
                "https://example.com/activity.png",
                10,
                10,
                BigDecimal.ZERO,
                0,
                "SINGLE",
                1,
                codeSourceMode,
                "IMMEDIATE",
                "PUBLISHED"
        );
        return jdbcTemplate.queryForObject("select max(id) from activity_product", Long.class);
    }

    private void insertImportedCode(Long activityId, String code) {
        jdbcTemplate.update("""
                        insert into redeem_code (
                          activity_id, code, source_type, batch_no, status, is_deleted
                        ) values (?, ?, 'THIRD_PARTY_IMPORTED', 'BATCH-001', 'AVAILABLE', 0)
                        """,
                activityId,
                code
        );
    }

    private void insertOrder(
            String orderNo,
            Long activityId,
            Long userId,
            String requestId,
            String purchaseUniqueKey,
            String orderStatus,
            String payStatus,
            String codeStatus,
            String failReason
    ) {
        jdbcTemplate.update("""
                        insert into order_record (
                          order_no, activity_id, user_id, request_id, purchase_unique_key, order_status,
                          pay_status, code_status, price_amount, fail_reason, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                orderNo,
                activityId,
                userId,
                requestId,
                purchaseUniqueKey,
                orderStatus,
                payStatus,
                codeStatus,
                BigDecimal.ZERO,
                failReason
        );
    }
}
