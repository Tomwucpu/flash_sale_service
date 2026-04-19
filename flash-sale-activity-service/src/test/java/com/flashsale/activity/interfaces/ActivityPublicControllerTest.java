package com.flashsale.activity.interfaces;

import com.flashsale.activity.FlashSaleActivityApplication;
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

@SpringBootTest(classes = FlashSaleActivityApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActivityPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from redeem_code_import_fail_detail");
        jdbcTemplate.update("delete from redeem_code_import_batch");
        jdbcTemplate.update("delete from redeem_code");
        jdbcTemplate.update("delete from activity_product");
    }

    @Test
    void publicListReturnsOnlyPublishedActivities() throws Exception {
        Long previewActivityId = insertActivity("公开预告活动", "PUBLISHED",
                nowMinusMinutes(30), nowPlusMinutes(30), nowPlusMinutes(60), 10);
        Long endedActivityId = insertActivity("公开已结束活动", "PUBLISHED",
                nowMinusMinutes(90), nowMinusMinutes(60), nowMinusMinutes(10), 20);
        insertActivity("未发布活动", "UNPUBLISHED",
                nowMinusMinutes(5), nowPlusMinutes(10), nowPlusMinutes(20), 30);
        insertActivity("已下线活动", "OFFLINE",
                nowMinusMinutes(50), nowMinusMinutes(30), nowPlusMinutes(20), 40);

        mockMvc.perform(get("/api/public/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(previewActivityId))
                .andExpect(jsonPath("$.data[0].phase").value("PREVIEW"))
                .andExpect(jsonPath("$.data[1].id").value(endedActivityId))
                .andExpect(jsonPath("$.data[1].phase").value("ENDED"));
    }

    @Test
    void publicDetailReturnsPublishedActivityOnly() throws Exception {
        Long activityId = insertActivity("公开详情活动", "PUBLISHED",
                nowMinusMinutes(15), nowPlusMinutes(15), nowPlusMinutes(45), 18);

        mockMvc.perform(get("/api/public/activities/{activityId}", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(activityId))
                .andExpect(jsonPath("$.data.title").value("公开详情活动"))
                .andExpect(jsonPath("$.data.phase").value("PREVIEW"));
    }

    @Test
    void publicDetailTreatsUnpublishedOrOfflineActivitiesAsMissing() throws Exception {
        Long unpublishedId = insertActivity("未发布详情活动", "UNPUBLISHED",
                nowMinusMinutes(5), nowPlusMinutes(15), nowPlusMinutes(45), 12);
        Long offlineId = insertActivity("已下线详情活动", "OFFLINE",
                nowMinusMinutes(50), nowMinusMinutes(10), nowPlusMinutes(20), 12);

        mockMvc.perform(get("/api/public/activities/{activityId}", unpublishedId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("活动不存在"));

        mockMvc.perform(get("/api/public/activities/{activityId}", offlineId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("活动不存在"));
    }

    private Long insertActivity(
            String title,
            String publishStatus,
            LocalDateTime publishTime,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int stock
    ) {
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
                stock,
                stock,
                BigDecimal.ZERO,
                0,
                "SINGLE",
                1,
                "SYSTEM_GENERATED",
                "IMMEDIATE",
                publishStatus,
                publishTime,
                startTime,
                endTime
        );
        return jdbcTemplate.queryForObject("select max(id) from activity_product", Long.class);
    }

    private LocalDateTime nowPlusMinutes(long minutes) {
        return LocalDateTime.now().plusMinutes(minutes);
    }

    private LocalDateTime nowMinusMinutes(long minutes) {
        return LocalDateTime.now().minusMinutes(minutes);
    }
}
