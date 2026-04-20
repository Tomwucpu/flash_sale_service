package com.flashsale.order.application;

import com.flashsale.common.mq.event.DomainEvent;
import com.flashsale.order.FlashSaleOrderApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FlashSaleOrderApplication.class)
@ActiveProfiles("test")
class ExportTaskConsumerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Autowired(required = false)
    private ExportTaskConsumer exportTaskConsumer;

    @Autowired(required = false)
    private ExportTaskDeadLetterConsumer exportTaskDeadLetterConsumer;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("delete from compensation_record");
        jdbcTemplate.update("delete from audit_log");
        jdbcTemplate.update("delete from export_task");
        jdbcTemplate.update("delete from redeem_code");
        jdbcTemplate.update("delete from order_record");
        jdbcTemplate.update("delete from activity_product");
        Files.createDirectories(Path.of("target/test-exports"));
        try (var paths = Files.list(Path.of("target/test-exports"))) {
            paths.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // test cleanup best effort
                }
            });
        }
    }

    @Test
    void exportGenerateConsumerMarksTaskSuccessAndCreatesFile() throws Exception {
        Long activityId = insertActivity();
        Long orderId = insertOrder("SO202604200401", activityId, 4001L, "CONFIRMED", "PAID", "ISSUED");
        insertAssignedCode(activityId, orderId, 4001L, "EXPORT-CODE-001");
        Long taskId = insertExportTask(activityId, 1L, "CSV", "{\"payStatus\":\"PAID\"}");

        exportTaskConsumer.onExportGenerate(exportEvent(taskId));

        Map<String, Object> task = jdbcTemplate.queryForMap("select * from export_task where id = ?", taskId);
        assertThat(task).containsEntry("status", "SUCCESS");
        assertThat(task.get("file_url")).isNotNull();

        String fileUrl = (String) task.get("file_url");
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        Path generatedFile = Path.of("target/test-exports").resolve(fileName);
        assertThat(Files.exists(generatedFile)).isTrue();
        assertThat(Files.readString(generatedFile))
                .contains("SO202604200401")
                .contains("EXPORT-CODE-001");

        assertThat(jdbcTemplate.queryForObject(
                "select count(1) from audit_log where biz_type = 'EXPORT_TASK' and biz_key = ?",
                Long.class,
                String.valueOf(taskId)
        )).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void deadLetterConsumerCreatesPendingCompensationLedger() {
        exportTaskDeadLetterConsumer.onDeadLetter(deadLetterEvent("export-task:999", 999L, "导出文件生成失败"));

        Map<String, Object> record = jdbcTemplate.queryForMap("select * from compensation_record where biz_key = ?", "export-task:999");
        assertThat(record)
                .containsEntry("biz_type", "EXPORT_TASK")
                .containsEntry("source_event", "export.generate.dead")
                .containsEntry("status", "PENDING")
                .containsEntry("reason", "导出文件生成失败");
    }

    private DomainEvent<Map<String, Object>> exportEvent(Long taskId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", taskId);
        return DomainEvent.create("export.generate", "export-task:" + taskId, payload, clock);
    }

    private DomainEvent<Map<String, Object>> deadLetterEvent(String bizKey, Long taskId, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", taskId);
        payload.put("reason", reason);
        return DomainEvent.create("export.generate.dead", bizKey, payload, clock);
    }

    private Long insertActivity() {
        jdbcTemplate.update("""
                        insert into activity_product (
                          title, description, cover_url, total_stock, available_stock, price_amount, need_payment,
                          purchase_limit_type, purchase_limit_count, code_source_mode, publish_mode, publish_status,
                          publish_time, start_time, end_time, version, is_deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, DATEADD('HOUR', 2, CURRENT_TIMESTAMP), 0, 0)
                        """,
                "导出任务活动",
                "导出任务活动描述",
                "https://example.com/export-task.png",
                10,
                10,
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
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 0)
                        """,
                orderNo,
                activityId,
                userId,
                "REQ-" + orderNo,
                "activity:%d:user:%d:req:%s".formatted(activityId, userId, "REQ-" + orderNo),
                orderStatus,
                payStatus,
                codeStatus,
                BigDecimal.TEN,
                null
        );
        return jdbcTemplate.queryForObject("select max(id) from order_record", Long.class);
    }

    private void insertAssignedCode(Long activityId, Long orderId, Long userId, String code) {
        jdbcTemplate.update("""
                        insert into redeem_code (
                          activity_id, code, source_type, status, assigned_user_id, assigned_order_id, assigned_at, is_deleted
                        ) values (?, ?, 'THIRD_PARTY_IMPORTED', 'ASSIGNED', ?, ?, CURRENT_TIMESTAMP, 0)
                        """,
                activityId,
                code,
                userId,
                orderId
        );
    }

    private Long insertExportTask(Long activityId, Long operatorId, String format, String filtersJson) {
        jdbcTemplate.update("""
                        insert into export_task (
                          activity_id, operator_id, format, filters_json, status, created_by, updated_by, is_deleted
                        ) values (?, ?, ?, ?, 'INIT', ?, ?, 0)
                        """,
                activityId,
                operatorId,
                format,
                filtersJson,
                operatorId,
                operatorId
        );
        return jdbcTemplate.queryForObject("select max(id) from export_task", Long.class);
    }
}
