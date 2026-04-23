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
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = FlashSaleOrderApplication.class)
@ActiveProfiles("test")
class PaymentOrderEventConsumerTest {

    @Autowired
    private PaymentOrderEventConsumer paymentOrderEventConsumer;

    @Autowired
    private OrderTimeoutCloseConsumer orderTimeoutCloseConsumer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private RedeemCodeGenerator redeemCodeGenerator;

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
    void paymentSuccessIssuesImportedCodeAndWritesSuccessResult() {
        Long activityId = insertActivity("THIRD_PARTY_IMPORTED", true, new BigDecimal("19.90"));
        Long orderId = insertOrder("SO202604190201", activityId, 2101L, "REQ-PAY-001", "INIT", "WAIT_PAY", "PENDING", new BigDecimal("19.90"));
        insertImportedCode(activityId, "PAY-CODE-001");

        paymentOrderEventConsumer.onPaymentSuccess(paymentSuccessEvent("SO202604190201", "TXN-202604190201"));

        Map<String, Object> order = jdbcTemplate.queryForMap("select * from order_record where id = ?", orderId);
        assertThat(order)
                .containsEntry("order_status", "CONFIRMED")
                .containsEntry("pay_status", "PAID")
                .containsEntry("code_status", "ISSUED")
                .containsEntry("fail_reason", null);

        Map<String, Object> code = jdbcTemplate.queryForMap("select * from redeem_code where code = ?", "PAY-CODE-001");
        assertThat(code)
                .containsEntry("status", "ASSIGNED")
                .containsEntry("assigned_user_id", 2101L)
                .containsEntry("assigned_order_id", orderId);

        ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq(RedisKeys.seckillResult(activityId, 2101L)), resultCaptor.capture());
        assertThat(resultCaptor.getValue())
                .containsEntry("status", "SUCCESS")
                .containsEntry("orderNo", "SO202604190201")
                .containsEntry("message", "抢购成功")
                .containsEntry("code", "PAY-CODE-001");
        verify(valueOperations, never()).increment(anyString());
        verify(valueOperations, never()).decrement(anyString());
    }

    @Test
    void duplicatePaymentSuccessDoesNotIssueSecondCode() {
        Long activityId = insertActivity("SYSTEM_GENERATED", true, new BigDecimal("29.90"));
        insertOrder("SO202604190202", activityId, 2102L, "REQ-PAY-002", "INIT", "WAIT_PAY", "PENDING", new BigDecimal("29.90"));
        when(redeemCodeGenerator.nextCode()).thenReturn("PAY-SYSTEM-CODE-001");

        DomainEvent<Map<String, Object>> event = paymentSuccessEvent("SO202604190202", "TXN-202604190202");
        paymentOrderEventConsumer.onPaymentSuccess(event);
        paymentOrderEventConsumer.onPaymentSuccess(event);

        assertThat(jdbcTemplate.queryForObject("select count(1) from redeem_code where assigned_user_id = 2102", Long.class))
                .isEqualTo(1L);
        Map<String, Object> order = jdbcTemplate.queryForMap("select * from order_record where order_no = ?", "SO202604190202");
        assertThat(order)
                .containsEntry("order_status", "CONFIRMED")
                .containsEntry("pay_status", "PAID")
                .containsEntry("code_status", "ISSUED");
    }

    @Test
    void timeoutCloseClosesWaitPayOrderAndCompensatesRedis() {
        Long activityId = insertActivity("THIRD_PARTY_IMPORTED", true, new BigDecimal("39.90"), 9);
        insertOrder("SO202604190203", activityId, 2103L, "REQ-PAY-003", "INIT", "WAIT_PAY", "PENDING", new BigDecimal("39.90"));
        when(valueOperations.decrement(RedisKeys.seckillLimit(activityId, 2103L))).thenReturn(0L);

        orderTimeoutCloseConsumer.onOrderTimeoutClose(orderTimeoutEvent("SO202604190203"));

        Map<String, Object> order = jdbcTemplate.queryForMap("select * from order_record where order_no = ?", "SO202604190203");
        assertThat(order)
                .containsEntry("order_status", "CLOSED")
                .containsEntry("pay_status", "CLOSED")
                .containsEntry("code_status", "PENDING")
                .containsEntry("fail_reason", "PAYMENT_TIMEOUT");

        ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq(RedisKeys.seckillResult(activityId, 2103L)), resultCaptor.capture());
        assertThat(resultCaptor.getValue())
                .containsEntry("status", "FAIL")
                .containsEntry("orderNo", "SO202604190203")
                .containsEntry("message", "支付超时，订单已关闭")
                .containsEntry("code", "");
        assertThat(jdbcTemplate.queryForObject(
                "select available_stock from activity_product where id = ?",
                Integer.class,
                activityId
        )).isEqualTo(10);
        verify(valueOperations).increment(RedisKeys.seckillStock(activityId));
        verify(valueOperations).decrement(RedisKeys.seckillLimit(activityId, 2103L));
    }

    private DomainEvent<Map<String, Object>> paymentSuccessEvent(String orderNo, String transactionNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNo", orderNo);
        payload.put("transactionNo", transactionNo);
        return new DomainEvent<>("message-id-" + transactionNo, "payment.success", orderNo, null, payload);
    }

    private DomainEvent<Map<String, Object>> orderTimeoutEvent(String orderNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNo", orderNo);
        return new DomainEvent<>("timeout-" + orderNo, "order.timeout.close", orderNo, null, payload);
    }

    private Long insertActivity(String codeSourceMode, boolean needPayment, BigDecimal priceAmount) {
        return insertActivity(codeSourceMode, needPayment, priceAmount, 10);
    }

    private Long insertActivity(String codeSourceMode, boolean needPayment, BigDecimal priceAmount, int availableStock) {
        jdbcTemplate.update("""
                        insert into activity_product (
                          title, description, cover_url, total_stock, available_stock, price_amount, need_payment,
                          purchase_limit_type, purchase_limit_count, code_source_mode, publish_mode, publish_status,
                          publish_time, start_time, end_time, version, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, DATEADD('HOUR', 2, CURRENT_TIMESTAMP), 0, 0)
                        """,
                "Task7活动",
                "Task7活动描述",
                "https://example.com/task7.png",
                10,
                availableStock,
                priceAmount,
                needPayment ? 1 : 0,
                "SINGLE",
                1,
                codeSourceMode,
                "IMMEDIATE",
                "PUBLISHED"
        );
        return jdbcTemplate.queryForObject("select max(id) from activity_product", Long.class);
    }

    private Long insertOrder(
            String orderNo,
            Long activityId,
            Long userId,
            String requestId,
            String orderStatus,
            String payStatus,
            String codeStatus,
            BigDecimal priceAmount
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
                "activity:%d:user:%d:req:%s".formatted(activityId, userId, requestId),
                orderStatus,
                payStatus,
                codeStatus,
                priceAmount,
                null
        );
        return jdbcTemplate.queryForObject("select max(id) from order_record", Long.class);
    }

    private void insertImportedCode(Long activityId, String code) {
        jdbcTemplate.update("""
                        insert into redeem_code (
                          activity_id, code, source_type, batch_no, status, is_deleted
                        ) values (?, ?, 'THIRD_PARTY_IMPORTED', 'BATCH-201', 'AVAILABLE', 0)
                        """,
                activityId,
                code
        );
    }
}
