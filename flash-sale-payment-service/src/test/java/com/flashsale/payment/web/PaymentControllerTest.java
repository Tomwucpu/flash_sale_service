package com.flashsale.payment.web;

import com.flashsale.common.security.context.UserContext;
import com.flashsale.payment.FlashSalePaymentApplication;
import com.flashsale.payment.application.PaymentTransactionNoGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FlashSalePaymentApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private PaymentTransactionNoGenerator paymentTransactionNoGenerator;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from payment_record");
        jdbcTemplate.update("delete from order_record");
        jdbcTemplate.update("delete from activity_product");
    }

    @Test
    void ownerCanCreateMockPaymentOrder() throws Exception {
        Long activityId = insertActivity(true, new BigDecimal("59.90"));
        insertOrder("SO202604190301", activityId, 2301L, "WAIT_PAY", "PENDING", new BigDecimal("59.90"));
        when(paymentTransactionNoGenerator.nextTransactionNo()).thenReturn("TXN-202604190301");

        mockMvc.perform(post("/api/payments/orders/{orderNo}", "SO202604190301")
                        .header(UserContext.USER_ID_HEADER, 2301L)
                        .header(UserContext.USERNAME_HEADER, "buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-PAYMENT-301"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.requestId").value("REQ-PAYMENT-301"))
                .andExpect(jsonPath("$.data.orderNo").value("SO202604190301"))
                .andExpect(jsonPath("$.data.transactionNo").value("TXN-202604190301"))
                .andExpect(jsonPath("$.data.payAmount").value(59.90))
                .andExpect(jsonPath("$.data.payStatus").value("INIT"));

        assertThat(jdbcTemplate.queryForMap("select * from payment_record where transaction_no = ?", "TXN-202604190301"))
                .containsEntry("order_no", "SO202604190301")
                .containsEntry("pay_status", "INIT")
                .containsEntry("pay_amount", new BigDecimal("59.90"));
        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                org.mockito.ArgumentMatchers.<Object>any(),
                org.mockito.ArgumentMatchers.<MessagePostProcessor>any()
        );
    }

    @Test
    void callbackMarksPaymentSuccessAndPublishesPaymentSuccessEvent() throws Exception {
        Long activityId = insertActivity(true, new BigDecimal("79.90"));
        insertOrder("SO202604190302", activityId, 2302L, "WAIT_PAY", "PENDING", new BigDecimal("79.90"));
        insertPayment("SO202604190302", "TXN-202604190302", new BigDecimal("79.90"), "INIT");

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "orderNo": "SO202604190302",
                                  "transactionNo": "TXN-202604190302"
                                }
                                """)
                        .header("X-Request-Id", "REQ-PAYMENT-302"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.orderNo").value("SO202604190302"))
                .andExpect(jsonPath("$.data.transactionNo").value("TXN-202604190302"))
                .andExpect(jsonPath("$.data.payStatus").value("SUCCESS"));

        assertThat(jdbcTemplate.queryForMap("select * from payment_record where transaction_no = ?", "TXN-202604190302"))
                .containsEntry("order_no", "SO202604190302")
                .containsEntry("pay_status", "SUCCESS");
        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                org.mockito.ArgumentMatchers.<Object>any()
        );
    }

    @Test
    void duplicateCallbackIsIdempotent() throws Exception {
        Long activityId = insertActivity(true, new BigDecimal("99.90"));
        insertOrder("SO202604190303", activityId, 2303L, "WAIT_PAY", "PENDING", new BigDecimal("99.90"));
        insertPayment("SO202604190303", "TXN-202604190303", new BigDecimal("99.90"), "INIT");

        String body = """
                {
                  "orderNo": "SO202604190303",
                  "transactionNo": "TXN-202604190303"
                }
                """;

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(APPLICATION_JSON)
                        .content(body)
                        .header("X-Request-Id", "REQ-PAYMENT-303-A"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(APPLICATION_JSON)
                        .content(body)
                        .header("X-Request-Id", "REQ-PAYMENT-303-B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payStatus").value("SUCCESS"));

        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(),
                anyString(),
                org.mockito.ArgumentMatchers.<Object>any()
        );
    }

    private Long insertActivity(boolean needPayment, BigDecimal priceAmount) {
        jdbcTemplate.update("""
                        insert into activity_product (
                          title, description, cover_url, total_stock, available_stock, price_amount, need_payment,
                          purchase_limit_type, purchase_limit_count, code_source_mode, publish_mode, publish_status,
                          publish_time, start_time, end_time, version, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, DATEADD('HOUR', 2, CURRENT_TIMESTAMP), 0, 0)
                        """,
                "支付活动",
                "支付活动描述",
                "https://example.com/pay.png",
                10,
                10,
                priceAmount,
                needPayment ? 1 : 0,
                "SINGLE",
                1,
                "THIRD_PARTY_IMPORTED",
                "IMMEDIATE",
                "PUBLISHED"
        );
        return jdbcTemplate.queryForObject("select max(id) from activity_product", Long.class);
    }

    private void insertOrder(
            String orderNo,
            Long activityId,
            Long userId,
            String payStatus,
            String codeStatus,
            BigDecimal priceAmount
    ) {
        jdbcTemplate.update("""
                        insert into order_record (
                          order_no, activity_id, user_id, request_id, purchase_unique_key, order_status,
                          pay_status, code_status, price_amount, fail_reason, is_deleted
                        ) values (?, ?, ?, ?, ?, 'INIT', ?, ?, ?, null, 0)
                        """,
                orderNo,
                activityId,
                userId,
                "REQ-" + orderNo,
                "activity:%d:user:%d:req:%s".formatted(activityId, userId, "REQ-" + orderNo),
                payStatus,
                codeStatus,
                priceAmount
        );
    }

    private void insertPayment(String orderNo, String transactionNo, BigDecimal payAmount, String payStatus) {
        jdbcTemplate.update("""
                        insert into payment_record (
                          order_no, transaction_no, pay_amount, pay_status, is_deleted
                        ) values (?, ?, ?, ?, 0)
                        """,
                orderNo,
                transactionNo,
                payAmount,
                payStatus
        );
    }
}
