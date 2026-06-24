package com.flashsale.order.controller;

import com.flashsale.common.security.context.UserContext;
import com.flashsale.order.FlashSaleOrderApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        FlashSaleOrderApplication.class,
        PublisherDashboardControllerTest.FixedClockConfig.class
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublisherDashboardControllerTest {

    private static final Long PUBLISHER_ID = 2001L;
    private static final Long OTHER_PUBLISHER_ID = 9999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from compensation_record");
        jdbcTemplate.update("delete from redeem_code");
        jdbcTemplate.update("delete from order_record");
        jdbcTemplate.update("delete from activity_product");
    }

    @Test
    void emptyPublisherReturnsRedesignedZeroState() throws Exception {
        mockMvc.perform(get("/api/dashboard/publisher")
                        .param("granularity", "week")
                        .header(UserContext.USER_ID_HEADER, PUBLISHER_ID)
                        .header(UserContext.USERNAME_HEADER, "publisher")
                        .header(UserContext.ROLE_HEADER, "PUBLISHER")
                        .header("X-Request-Id", "REQ-DASH-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.summary.revenue").value(0))
                .andExpect(jsonPath("$.data.summary.avgOrderValue").value(0))
                .andExpect(jsonPath("$.data.summary.totalOrders").value(0))
                .andExpect(jsonPath("$.data.summary.paidOrders").value(0))
                .andExpect(jsonPath("$.data.summary.paidOrderRate").value(0))
                .andExpect(jsonPath("$.data.summary.inventoryConsumed").value(0))
                .andExpect(jsonPath("$.data.summary.inventoryTotal").value(0))
                .andExpect(jsonPath("$.data.summary.inventoryConsumptionRate").value(0))
                .andExpect(jsonPath("$.data.summary.highConsumptionActivityCount").value(0))
                .andExpect(jsonPath("$.data.summary.pendingCompensations").value(0))
                .andExpect(jsonPath("$.data.trend.granularity").value("week"))
                .andExpect(jsonPath("$.data.trend.periodLabel").value("2026-05-04 至 2026-06-28"))
                .andExpect(jsonPath("$.data.trend.buckets.length()").value(8))
                .andExpect(jsonPath("$.data.activityPerformance.length()").value(0))
                .andExpect(jsonPath("$.data.insights.highConsumptionCount").value(0))
                .andExpect(jsonPath("$.data.insights.mediumConsumptionCount").value(0))
                .andExpect(jsonPath("$.data.insights.lowConsumptionCount").value(0))
                .andExpect(jsonPath("$.data.insights.messages[0]").value("当前周期暂无经营数据"));
    }

    @Test
    void weekGranularityReturnsSummaryTrendActivitiesAndInsights() throws Exception {
        Long activityA = insertActivity(PUBLISHER_ID, "活动A", "PUBLISHED", 100, 10,
                LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 6, 30, 23, 59));
        Long activityB = insertActivity(PUBLISHER_ID, "活动B", "PUBLISHED", 200, 100,
                LocalDateTime.of(2026, 5, 20, 10, 0),
                LocalDateTime.of(2026, 7, 10, 23, 59));
        Long activityC = insertActivity(PUBLISHER_ID, "活动C", "OFFLINE", 100, 85,
                LocalDateTime.of(2026, 4, 1, 10, 0),
                LocalDateTime.of(2026, 5, 15, 23, 59));

        Long otherPublisherActivity = insertActivity(OTHER_PUBLISHER_ID, "其他活动", "PUBLISHED", 100, 10,
                LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 6, 30, 23, 59));

        insertOrder("CUR-A-1", activityA, 3001L, "CONFIRMED", "PAID", "ISSUED",
                new BigDecimal("100.00"), null, LocalDateTime.of(2026, 6, 22, 10, 0));
        insertOrder("CUR-A-2", activityA, 3002L, "CONFIRMED", "PAID", "ISSUED",
                new BigDecimal("200.00"), null, LocalDateTime.of(2026, 6, 23, 11, 0));
        insertOrder("CUR-A-3", activityA, 3003L, "INIT", "WAIT_PAY", "PENDING",
                new BigDecimal("150.00"), null, LocalDateTime.of(2026, 6, 23, 12, 0));
        insertOrder("CUR-B-1", activityB, 3004L, "CONFIRMED", "PAID", "ISSUED",
                new BigDecimal("50.00"), null, LocalDateTime.of(2026, 6, 18, 9, 0));
        insertOrder("CUR-B-2", activityB, 3005L, "INIT", "WAIT_PAY", "PENDING",
                new BigDecimal("70.00"), null, LocalDateTime.of(2026, 6, 24, 14, 0));

        insertOrder("PRE-A-1", activityA, 3010L, "CONFIRMED", "PAID", "ISSUED",
                new BigDecimal("80.00"), null, LocalDateTime.of(2026, 4, 10, 10, 0));
        insertOrder("PRE-A-2", activityA, 3011L, "INIT", "WAIT_PAY", "PENDING",
                new BigDecimal("120.00"), null, LocalDateTime.of(2026, 4, 12, 10, 0));
        insertOrder("PRE-B-1", activityB, 3012L, "CONFIRMED", "PAID", "ISSUED",
                new BigDecimal("20.00"), null, LocalDateTime.of(2026, 4, 18, 10, 0));

        insertOrder("OTHER-1", otherPublisherActivity, 4001L, "CONFIRMED", "PAID", "ISSUED",
                new BigDecimal("999.00"), null, LocalDateTime.of(2026, 6, 24, 9, 0));

        insertCompensationRecord("PENDING");
        insertCompensationRecord("RESOLVED");

        mockMvc.perform(get("/api/dashboard/publisher")
                        .param("granularity", "week")
                        .header(UserContext.USER_ID_HEADER, PUBLISHER_ID)
                        .header(UserContext.USERNAME_HEADER, "publisher")
                        .header(UserContext.ROLE_HEADER, "PUBLISHER")
                        .header("X-Request-Id", "REQ-DASH-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.summary.revenue").value(350.00))
                .andExpect(jsonPath("$.data.summary.revenueChangeRate").value(2.5))
                .andExpect(jsonPath("$.data.summary.avgOrderValue").value(116.67))
                .andExpect(jsonPath("$.data.summary.totalOrders").value(5))
                .andExpect(jsonPath("$.data.summary.totalOrdersChangeRate").value(0.6667))
                .andExpect(jsonPath("$.data.summary.paidOrders").value(3))
                .andExpect(jsonPath("$.data.summary.paidOrdersChangeRate").value(0.5))
                .andExpect(jsonPath("$.data.summary.paidOrderRate").value(0.6))
                .andExpect(jsonPath("$.data.summary.inventoryConsumed").value(205))
                .andExpect(jsonPath("$.data.summary.inventoryTotal").value(400))
                .andExpect(jsonPath("$.data.summary.inventoryConsumptionRate").value(0.5125))
                .andExpect(jsonPath("$.data.summary.highConsumptionActivityCount").value(1))
                .andExpect(jsonPath("$.data.summary.pendingCompensations").value(1))
                .andExpect(jsonPath("$.data.trend.granularity").value("week"))
                .andExpect(jsonPath("$.data.trend.periodLabel").value("2026-05-04 至 2026-06-28"))
                .andExpect(jsonPath("$.data.trend.buckets.length()").value(8))
                .andExpect(jsonPath("$.data.trend.buckets[6].label").value("06-15 至 06-21"))
                .andExpect(jsonPath("$.data.trend.buckets[6].revenue").value(50.00))
                .andExpect(jsonPath("$.data.trend.buckets[6].totalOrders").value(1))
                .andExpect(jsonPath("$.data.trend.buckets[6].paidOrders").value(1))
                .andExpect(jsonPath("$.data.trend.buckets[7].label").value("06-22 至 06-28"))
                .andExpect(jsonPath("$.data.trend.buckets[7].revenue").value(300.00))
                .andExpect(jsonPath("$.data.trend.buckets[7].totalOrders").value(4))
                .andExpect(jsonPath("$.data.trend.buckets[7].paidOrders").value(2))
                .andExpect(jsonPath("$.data.activityPerformance.length()").value(3))
                .andExpect(jsonPath("$.data.activityPerformance[0].activityId").value(activityA))
                .andExpect(jsonPath("$.data.activityPerformance[0].title").value("活动A"))
                .andExpect(jsonPath("$.data.activityPerformance[0].phase").value("ONGOING"))
                .andExpect(jsonPath("$.data.activityPerformance[0].revenue").value(300.00))
                .andExpect(jsonPath("$.data.activityPerformance[0].revenueChangeRate").value(2.75))
                .andExpect(jsonPath("$.data.activityPerformance[0].totalOrders").value(3))
                .andExpect(jsonPath("$.data.activityPerformance[0].totalOrdersChangeRate").value(0.5))
                .andExpect(jsonPath("$.data.activityPerformance[0].paidOrders").value(2))
                .andExpect(jsonPath("$.data.activityPerformance[0].paidOrderRate").value(0.6667))
                .andExpect(jsonPath("$.data.activityPerformance[0].inventoryConsumptionRate").value(0.9))
                .andExpect(jsonPath("$.data.activityPerformance[1].activityId").value(activityB))
                .andExpect(jsonPath("$.data.activityPerformance[1].revenue").value(50.00))
                .andExpect(jsonPath("$.data.activityPerformance[1].revenueChangeRate").value(1.5))
                .andExpect(jsonPath("$.data.activityPerformance[1].totalOrders").value(2))
                .andExpect(jsonPath("$.data.activityPerformance[1].totalOrdersChangeRate").value(1.0))
                .andExpect(jsonPath("$.data.activityPerformance[1].paidOrders").value(1))
                .andExpect(jsonPath("$.data.activityPerformance[1].paidOrderRate").value(0.5))
                .andExpect(jsonPath("$.data.activityPerformance[1].inventoryConsumptionRate").value(0.5))
                .andExpect(jsonPath("$.data.activityPerformance[2].activityId").value(activityC))
                .andExpect(jsonPath("$.data.activityPerformance[2].revenue").value(0))
                .andExpect(jsonPath("$.data.insights.highConsumptionCount").value(1))
                .andExpect(jsonPath("$.data.insights.mediumConsumptionCount").value(1))
                .andExpect(jsonPath("$.data.insights.lowConsumptionCount").value(1))
                .andExpect(jsonPath("$.data.insights.messages.length()").value(1))
                .andExpect(jsonPath("$.data.insights.messages[0]").value("1 个高营收活动库存消耗超过 90%，需关注供给风险"));
    }

    @Test
    void monthGranularityUsesMonthBucketsAndNaturalLabels() throws Exception {
        Long activityId = insertActivity(PUBLISHER_ID, "月度活动", "PUBLISHED", 120, 20,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 6, 30, 23, 59));

        insertOrder("MONTH-1", activityId, 3001L, "CONFIRMED", "PAID", "ISSUED",
                new BigDecimal("88.00"), null, LocalDateTime.of(2026, 6, 5, 9, 0));
        insertOrder("MONTH-2", activityId, 3002L, "CONFIRMED", "PAID", "ISSUED",
                new BigDecimal("66.00"), null, LocalDateTime.of(2026, 5, 5, 9, 0));

        mockMvc.perform(get("/api/dashboard/publisher")
                        .param("granularity", "month")
                        .header(UserContext.USER_ID_HEADER, PUBLISHER_ID)
                        .header(UserContext.USERNAME_HEADER, "publisher")
                        .header(UserContext.ROLE_HEADER, "PUBLISHER")
                        .header("X-Request-Id", "REQ-DASH-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trend.granularity").value("month"))
                .andExpect(jsonPath("$.data.trend.periodLabel").value("2026-01-01 至 2026-06-30"))
                .andExpect(jsonPath("$.data.trend.buckets.length()").value(6))
                .andExpect(jsonPath("$.data.trend.buckets[4].label").value("2026-05"))
                .andExpect(jsonPath("$.data.trend.buckets[4].revenue").value(66.00))
                .andExpect(jsonPath("$.data.trend.buckets[5].label").value("2026-06"))
                .andExpect(jsonPath("$.data.trend.buckets[5].revenue").value(88.00));
    }

    @Test
    void unauthorizedUserIsRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard/publisher")
                        .param("granularity", "week")
                        .header(UserContext.USER_ID_HEADER, 3001L)
                        .header(UserContext.USERNAME_HEADER, "buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-DASH-004"))
                .andExpect(status().isForbidden());
    }

    private Long insertActivity(Long createdBy, String title, String publishStatus, int totalStock, int availableStock,
                                LocalDateTime startTime, LocalDateTime endTime) {
        jdbcTemplate.update("""
                        insert into activity_product (
                          title, description, cover_url, total_stock, available_stock, price_amount, need_payment,
                          purchase_limit_type, purchase_limit_count, code_source_mode, publish_mode, publish_status,
                          publish_time, start_time, end_time, version, is_deleted, created_by, created_at, updated_at
                        ) values (?, '', '', ?, ?, 29.90, 1, 'SINGLE', 1, 'SYSTEM_GENERATED', 'IMMEDIATE', ?,
                                  CURRENT_TIMESTAMP, ?, ?, 0, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                title, totalStock, availableStock, publishStatus, startTime, endTime, createdBy);
        return jdbcTemplate.queryForObject(
                "select max(id) from activity_product where created_by = ?",
                Long.class,
                createdBy
        );
    }

    private void insertOrder(String orderNo, Long activityId, Long userId,
                             String orderStatus, String payStatus, String codeStatus,
                             BigDecimal priceAmount, String failReason, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                        insert into order_record (
                          order_no, activity_id, user_id, request_id, purchase_unique_key, order_status,
                          pay_status, code_status, price_amount, fail_reason, created_at, updated_at, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                orderNo, activityId, userId, "REQ-" + orderNo,
                "activity:%d:user:%d:req:%s".formatted(activityId, userId, "REQ-" + orderNo),
                orderStatus, payStatus, codeStatus, priceAmount, failReason, createdAt, createdAt);
    }

    private void insertCompensationRecord(String status) {
        jdbcTemplate.update("""
                        insert into compensation_record (
                          biz_type, biz_key, source_event, status, reason, is_deleted
                        ) values ('ORDER_CREATE', 'COMP-TEST', 'ORDER_CREATE_EVENT', ?, 'test reason', 0)
                        """,
                status);
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-06-24T08:00:00Z"), ZoneId.of("Asia/Shanghai"));
        }
    }
}
