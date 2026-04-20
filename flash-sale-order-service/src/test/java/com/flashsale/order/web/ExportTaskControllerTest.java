package com.flashsale.order.web;

import com.flashsale.common.security.context.UserContext;
import com.flashsale.order.FlashSaleOrderApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FlashSaleOrderApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExportTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from compensation_record");
        jdbcTemplate.update("delete from audit_log");
        jdbcTemplate.update("delete from export_task");
        jdbcTemplate.update("delete from redeem_code");
        jdbcTemplate.update("delete from order_record");
        jdbcTemplate.update("delete from activity_product");
    }

    @Test
    void publisherCanCreateExportTask() throws Exception {
        Long activityId = insertActivity();

        mockMvc.perform(post("/api/exports/tasks")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "activityId": %d,
                                  "format": "CSV",
                                  "filters": {
                                    "payStatus": "PAID"
                                  }
                                }
                                """.formatted(activityId))
                        .header(UserContext.USER_ID_HEADER, 3001L)
                        .header(UserContext.USERNAME_HEADER, "publisher")
                        .header(UserContext.ROLE_HEADER, "PUBLISHER")
                        .header("X-Request-Id", "REQ-EXPORT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.requestId").value("REQ-EXPORT-001"))
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.data.format").value("CSV"))
                .andExpect(jsonPath("$.data.status").value("INIT"))
                .andExpect(jsonPath("$.data.fileUrl").doesNotExist());

        assertThat(jdbcTemplate.queryForMap("select * from export_task where activity_id = ?", activityId))
                .containsEntry("operator_id", 3001L)
                .containsEntry("format", "CSV")
                .containsEntry("status", "INIT");
    }

    @Test
    void normalUserCannotCreateExportTask() throws Exception {
        Long activityId = insertActivity();

        mockMvc.perform(post("/api/exports/tasks")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "activityId": %d,
                                  "format": "CSV"
                                }
                                """.formatted(activityId))
                        .header(UserContext.USER_ID_HEADER, 3002L)
                        .header(UserContext.USERNAME_HEADER, "buyer")
                        .header(UserContext.ROLE_HEADER, "USER")
                        .header("X-Request-Id", "REQ-EXPORT-002"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanQueryExportTaskDetail() throws Exception {
        Long activityId = insertActivity();
        jdbcTemplate.update("""
                        insert into export_task (
                          activity_id, operator_id, format, filters_json, status, file_url, fail_reason, created_by, updated_by, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                activityId,
                1L,
                "CSV",
                "{\"payStatus\":\"PAID\"}",
                "SUCCESS",
                "/api/exports/files/export-task-1.csv",
                null,
                1L,
                1L
        );
        Long taskId = jdbcTemplate.queryForObject("select max(id) from export_task", Long.class);

        mockMvc.perform(get("/api/exports/tasks/{taskId}", taskId)
                        .header(UserContext.USER_ID_HEADER, 1L)
                        .header(UserContext.USERNAME_HEADER, "admin")
                        .header(UserContext.ROLE_HEADER, "ADMIN")
                        .header("X-Request-Id", "REQ-EXPORT-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.fileUrl").value("/api/exports/files/export-task-1.csv"));
    }

    private Long insertActivity() {
        jdbcTemplate.update("""
                        insert into activity_product (
                          title, description, cover_url, total_stock, available_stock, price_amount, need_payment,
                          purchase_limit_type, purchase_limit_count, code_source_mode, publish_mode, publish_status,
                          publish_time, start_time, end_time, version, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, DATEADD('HOUR', 2, CURRENT_TIMESTAMP), 0, 0)
                        """,
                "导出活动",
                "导出活动描述",
                "https://example.com/export.png",
                20,
                20,
                BigDecimal.TEN,
                1,
                "SINGLE",
                1,
                "THIRD_PARTY_IMPORTED",
                "IMMEDIATE",
                "PUBLISHED"
        );
        return jdbcTemplate.queryForObject("select max(id) from activity_product", Long.class);
    }
}
