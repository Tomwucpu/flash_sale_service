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
class OrderCodeControllerTest {

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
    void ownerCanQueryIssuedCodeByOrderNo() throws Exception {
        Long activityId = insertActivity();
        Long orderId = insertOrder("SO202604190101", activityId, 2001L, "CONFIRMED", "NO_NEED", "ISSUED");
        insertAssignedCode(activityId, orderId, 2001L, "QUERY-CODE-001");

        mockMvc.perform(get("/api/codes/orders/{orderNo}", "SO202604190101")
                        .header(UserContext.USER_ID_HEADER, 2001L)
                        .header(UserContext.USERNAME_HEADER, "buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-CODE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.requestId").value("REQ-CODE-001"))
                .andExpect(jsonPath("$.data.orderNo").value("SO202604190101"))
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.payStatus").value("NO_NEED"))
                .andExpect(jsonPath("$.data.codeStatus").value("ISSUED"))
                .andExpect(jsonPath("$.data.code").value("QUERY-CODE-001"));
    }

    @Test
    void unknownOrderReturnsBusinessNotFound() throws Exception {
        mockMvc.perform(get("/api/codes/orders/{orderNo}", "SO404")
                        .header(UserContext.USER_ID_HEADER, 2001L)
                        .header(UserContext.USERNAME_HEADER, "buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-CODE-002"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("订单不存在"));
    }

    @Test
    void nonOwnerQueryIsRejected() throws Exception {
        Long activityId = insertActivity();
        Long orderId = insertOrder("SO202604190102", activityId, 2001L, "CONFIRMED", "NO_NEED", "ISSUED");
        insertAssignedCode(activityId, orderId, 2001L, "QUERY-CODE-002");

        mockMvc.perform(get("/api/codes/orders/{orderNo}", "SO202604190102")
                        .header(UserContext.USER_ID_HEADER, 2002L)
                        .header(UserContext.USERNAME_HEADER, "other-buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-CODE-003"))
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
                "查询活动",
                "查询活动描述",
                "https://example.com/query.png",
                10,
                10,
                BigDecimal.ZERO,
                0,
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
            String codeStatus
    ) {
        jdbcTemplate.update("""
                        insert into order_record (
                          order_no, activity_id, user_id, request_id, purchase_unique_key, order_status,
                          pay_status, code_status, price_amount, fail_reason, updated_at, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TIMESTAMP '2026-04-19 10:05:00', 0)
                        """,
                orderNo,
                activityId,
                userId,
                "REQ-" + orderNo,
                "activity:%d:user:%d:req:%s".formatted(activityId, userId, "REQ-" + orderNo),
                orderStatus,
                payStatus,
                codeStatus,
                BigDecimal.ZERO,
                null
        );
        return jdbcTemplate.queryForObject("select max(id) from order_record", Long.class);
    }

    private void insertAssignedCode(Long activityId, Long orderId, Long userId, String code) {
        jdbcTemplate.update("""
                        insert into redeem_code (
                          activity_id, code, source_type, status, assigned_user_id, assigned_order_id, assigned_at, is_deleted
                        ) values (?, ?, 'SYSTEM_GENERATED', 'ASSIGNED', ?, ?, TIMESTAMP '2026-04-19 10:05:00', 0)
                        """,
                activityId,
                code,
                userId,
                orderId
        );
    }
}
