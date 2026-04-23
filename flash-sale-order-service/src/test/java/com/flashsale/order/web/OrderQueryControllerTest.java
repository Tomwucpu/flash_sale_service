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
import java.time.LocalDateTime;

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
    void ownerCanQueryAllOrdersInActivityWithCode() throws Exception {
        Long activityId = insertActivity();
        Long orderId1 = insertOrder(
                "SO202604200001",
                activityId,
                3001L,
                "CONFIRMED",
                "PAID",
                "ISSUED",
                new BigDecimal("29.90"),
                null,
                LocalDateTime.of(2026, 4, 20, 10, 5)
        );
        insertAssignedCode(activityId, orderId1, 3001L, "QUERY-CODE-001");
        insertOrder(
                "SO202604200002",
                activityId,
                3001L,
                "CLOSED",
                "CLOSED",
                "PENDING",
                new BigDecimal("9.90"),
                "PAYMENT_TIMEOUT",
                LocalDateTime.of(2026, 4, 20, 10, 15)
        );
        insertOrder(
                "SO202604200003",
                activityId,
                3002L,
                "CONFIRMED",
                "PAID",
                "ISSUED",
                new BigDecimal("9.90"),
                null,
                LocalDateTime.of(2026, 4, 20, 10, 20)
        );

        mockMvc.perform(get("/api/orders/activities/{activityId}", activityId)
                        .header(UserContext.USER_ID_HEADER, 3001L)
                        .header(UserContext.USERNAME_HEADER, "buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-ORDER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.requestId").value("REQ-ORDER-001"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].orderNo").value("SO202604200002"))
                .andExpect(jsonPath("$.data[0].activityId").value(activityId))
                .andExpect(jsonPath("$.data[0].userId").value(3001L))
                .andExpect(jsonPath("$.data[0].orderStatus").value("CLOSED"))
                .andExpect(jsonPath("$.data[0].payStatus").value("CLOSED"))
                .andExpect(jsonPath("$.data[0].codeStatus").value("PENDING"))
                .andExpect(jsonPath("$.data[0].priceAmount").value(9.90))
                .andExpect(jsonPath("$.data[0].failReason").value("PAYMENT_TIMEOUT"))
                .andExpect(jsonPath("$.data[0].code").doesNotExist())
                .andExpect(jsonPath("$.data[1].orderNo").value("SO202604200001"))
                .andExpect(jsonPath("$.data[1].orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data[1].payStatus").value("PAID"))
                .andExpect(jsonPath("$.data[1].codeStatus").value("ISSUED"))
                .andExpect(jsonPath("$.data[1].priceAmount").value(29.90))
                .andExpect(jsonPath("$.data[1].failReason").doesNotExist())
                .andExpect(jsonPath("$.data[1].code").value("QUERY-CODE-001"));
    }

    @Test
    void noOrderInActivityReturnsEmptyList() throws Exception {
        Long activityId = insertActivity();

        mockMvc.perform(get("/api/orders/activities/{activityId}", activityId)
                        .header(UserContext.USER_ID_HEADER, 3001L)
                        .header(UserContext.USERNAME_HEADER, "buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-ORDER-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void onlyCurrentActivityOrdersAreReturned() throws Exception {
        Long activityId1 = insertActivity();
        Long activityId2 = insertActivity();
        insertOrder(
                "SO202604200101",
                activityId1,
                3001L,
                "CONFIRMED",
                "PAID",
                "ISSUED",
                new BigDecimal("19.90"),
                null,
                LocalDateTime.of(2026, 4, 20, 10, 5)
        );
        insertOrder(
                "SO202604200102",
                activityId2,
                3001L,
                "CONFIRMED",
                "PAID",
                "ISSUED",
                new BigDecimal("39.90"),
                null,
                LocalDateTime.of(2026, 4, 20, 10, 15)
        );

        mockMvc.perform(get("/api/orders/activities/{activityId}", activityId1)
                        .header(UserContext.USER_ID_HEADER, 3001L)
                        .header(UserContext.USERNAME_HEADER, "buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-ORDER-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].orderNo").value("SO202604200101"))
                .andExpect(jsonPath("$.data[0].activityId").value(activityId1));
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

    private Long insertOrder(
            String orderNo,
            Long activityId,
            Long userId,
            String orderStatus,
            String payStatus,
            String codeStatus,
            BigDecimal priceAmount,
            String failReason,
            LocalDateTime updatedAt
    ) {
        jdbcTemplate.update("""
                        insert into order_record (
                          order_no, activity_id, user_id, request_id, purchase_unique_key, order_status,
                          pay_status, code_status, price_amount, fail_reason, updated_at, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
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
                failReason,
                updatedAt
        );
        return jdbcTemplate.queryForObject("select max(id) from order_record", Long.class);
    }

    private void insertAssignedCode(Long activityId, Long orderId, Long userId, String code) {
        jdbcTemplate.update("""
                        insert into redeem_code (
                          activity_id, code, source_type, status, assigned_user_id, assigned_order_id, assigned_at, is_deleted
                        ) values (?, ?, 'SYSTEM_GENERATED', 'ASSIGNED', ?, ?, TIMESTAMP '2026-04-20 10:05:00', 0)
                        """,
                activityId,
                code,
                userId,
                orderId
        );
    }
}
