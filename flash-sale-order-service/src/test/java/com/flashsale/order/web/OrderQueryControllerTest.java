package com.flashsale.order.web;

import com.flashsale.common.security.context.UserContext;
import com.flashsale.order.FlashSaleOrderApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FlashSaleOrderApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from redeem_code");
        jdbcTemplate.update("delete from order_record");
        jdbcTemplate.update("delete from activity_product");
    }

    @Test
    void ownerCanQueryOwnOrderDetail() throws Exception {
        Long activityId = insertActivity();
        insertOrder("SO202604200001", activityId, 3001L, "CONFIRMED", "PAID", "ISSUED", new BigDecimal("29.90"), null);

        mockMvc.perform(get("/api/orders/{orderNo}", "SO202604200001")
                        .header(UserContext.USER_ID_HEADER, 3001L)
                        .header(UserContext.USERNAME_HEADER, "buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-ORDER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.requestId").value("REQ-ORDER-001"))
                .andExpect(jsonPath("$.data.orderNo").value("SO202604200001"))
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.data.userId").value(3001L))
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.payStatus").value("PAID"))
                .andExpect(jsonPath("$.data.codeStatus").value("ISSUED"))
                .andExpect(jsonPath("$.data.priceAmount").value(29.90))
                .andExpect(jsonPath("$.data.failReason").doesNotExist());
    }

    @Test
    void unknownOrderReturnsBusinessNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/{orderNo}", "SO404")
                        .header(UserContext.USER_ID_HEADER, 3001L)
                        .header(UserContext.USERNAME_HEADER, "buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-ORDER-002"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("订单不存在"));
    }

    @Test
    void nonOwnerQueryIsRejected() throws Exception {
        Long activityId = insertActivity();
        insertOrder("SO202604200002", activityId, 3001L, "CLOSED", "CLOSED", "PENDING", new BigDecimal("9.90"), "PAYMENT_TIMEOUT");

        mockMvc.perform(get("/api/orders/{orderNo}", "SO202604200002")
                        .header(UserContext.USER_ID_HEADER, 3002L)
                        .header(UserContext.USERNAME_HEADER, "other-buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-ORDER-003"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("无权查看该订单"));
    }

    private Long insertActivity() {
        jdbcTemplate.update("""
                        insert into activity_product (
                          title, description, cover_url, total_stock, available_stock, price_amount, need_payment,
                          purchase_limit_type, purchase_limit_count, code_source_mode, publish_mode, publish_status,
                          publish_time, start_time, end_time, version, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, DATEADD('HOUR', 2, CURRENT_TIMESTAMP), 0, 0)
                        """,
                "订单查询活动",
                "订单查询活动描述",
                "https://example.com/order-query.png",
                10,
                10,
                new BigDecimal("29.90"),
                1,
                "SINGLE",
                1,
                "SYSTEM_GENERATED",
                "IMMEDIATE",
                "PUBLISHED"
        );
        return jdbcTemplate.queryForObject("select max(id) from activity_product", Long.class);
    }

    private void insertOrder(
            String orderNo,
            Long activityId,
            Long userId,
            String orderStatus,
            String payStatus,
            String codeStatus,
            BigDecimal priceAmount,
            String failReason
    ) {
        jdbcTemplate.update("""
                        insert into order_record (
                          order_no, activity_id, user_id, request_id, purchase_unique_key, order_status,
                          pay_status, code_status, price_amount, fail_reason, updated_at, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TIMESTAMP '2026-04-20 10:05:00', 0)
                        """,
                orderNo,
                activityId,
                userId,
                "REQ-" + orderNo,
                "activity:%d:user:%d:req:%s".formatted(activityId, userId, "REQ-" + orderNo),
                orderStatus,
                payStatus,
                codeStatus,
                priceAmount,
                failReason
        );
    }
}
