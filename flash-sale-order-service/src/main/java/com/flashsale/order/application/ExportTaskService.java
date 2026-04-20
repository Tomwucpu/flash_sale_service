package com.flashsale.order.application;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.mq.event.DomainEvent;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.order.domain.ActivityProductEntity;
import com.flashsale.order.domain.AuditLogEntity;
import com.flashsale.order.domain.CompensationRecordEntity;
import com.flashsale.order.domain.ExportTaskEntity;
import com.flashsale.order.mapper.ActivityProductMapper;
import com.flashsale.order.mapper.AuditLogMapper;
import com.flashsale.order.mapper.CompensationRecordMapper;
import com.flashsale.order.mapper.ExportTaskMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ExportTaskService {

    private static final String EXPORT_TASK_STATUS_INIT = "INIT";
    private static final String EXPORT_TASK_STATUS_PROCESSING = "PROCESSING";
    private static final String EXPORT_TASK_STATUS_SUCCESS = "SUCCESS";
    private static final String EXPORT_TASK_STATUS_FAILED = "FAILED";
    private static final String COMPENSATION_STATUS_PENDING = "PENDING";
    private static final String COMPENSATION_STATUS_RESOLVED = "RESOLVED";
    private static final String BIZ_TYPE_EXPORT_TASK = "EXPORT_TASK";
    private static final String BIZ_TYPE_COMPENSATION = "COMPENSATION_RECORD";
    private static final String EXPORT_EXCHANGE = "flash.sale.event.exchange";
    private static final String EXPORT_GENERATE_ROUTING_KEY = "export.generate";
    private static final String EXPORT_DEAD_ROUTING_KEY = "export.generate.dead";

    private final ActivityProductMapper activityProductMapper;
    private final ExportTaskMapper exportTaskMapper;
    private final AuditLogMapper auditLogMapper;
    private final CompensationRecordMapper compensationRecordMapper;
    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Path exportDirectory;

    public ExportTaskService(
            ActivityProductMapper activityProductMapper,
            ExportTaskMapper exportTaskMapper,
            AuditLogMapper auditLogMapper,
            CompensationRecordMapper compensationRecordMapper,
            RabbitTemplate rabbitTemplate,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${flash-sale.export.directory:./exports}") String exportDirectory
    ) {
        this.activityProductMapper = activityProductMapper;
        this.exportTaskMapper = exportTaskMapper;
        this.auditLogMapper = auditLogMapper;
        this.compensationRecordMapper = compensationRecordMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.exportDirectory = Paths.get(exportDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public ExportTaskView createTask(ExportTaskCreateCommand command) {
        ActivityProductEntity activity = loadActivity(command.activityId());
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        String format = normalizeFormat(command.format());
        ExportTaskEntity entity = new ExportTaskEntity();
        entity.setActivityId(command.activityId());
        entity.setOperatorId(command.operatorId());
        entity.setFormat(format);
        entity.setFiltersJson(toJson(command.filters()));
        entity.setStatus(EXPORT_TASK_STATUS_INIT);
        entity.setCreatedBy(command.operatorId());
        entity.setUpdatedBy(command.operatorId());
        entity.setIsDeleted(0);
        exportTaskMapper.insert(entity);

        recordAudit(
                BIZ_TYPE_EXPORT_TASK,
                String.valueOf(entity.getId()),
                "EXPORT_TASK_CREATED",
                command.operatorId(),
                command.requestId(),
                Map.of(
                        "activityId", entity.getActivityId(),
                        "format", entity.getFormat(),
                        "filters", command.filters()
                )
        );
        publishExportGenerate(entity.getId());
        return toView(exportTaskMapper.findByIdActive(entity.getId()));
    }

    public ExportTaskView getTask(Long taskId) {
        ExportTaskEntity entity = exportTaskMapper.findByIdActive(taskId);
        if (entity == null) {
            throw new IllegalArgumentException("导出任务不存在");
        }
        return toView(entity);
    }

    public List<ExportTaskView> listTasks(Long activityId) {
        List<ExportTaskEntity> entities = exportTaskMapper.findByActivityId(activityId);
        List<ExportTaskView> views = new ArrayList<>(entities.size());
        for (ExportTaskEntity entity : entities) {
            views.add(toView(entity));
        }
        return views;
    }

    public List<CompensationRecordView> listCompensations() {
        List<CompensationRecordEntity> entities = compensationRecordMapper.selectList(
                new LambdaQueryWrapper<CompensationRecordEntity>()
                        .eq(CompensationRecordEntity::getIsDeleted, 0)
                        .orderByDesc(CompensationRecordEntity::getId)
        );
        List<CompensationRecordView> views = new ArrayList<>(entities.size());
        for (CompensationRecordEntity entity : entities) {
            views.add(toView(entity));
        }
        return views;
    }

    @Transactional
    public CompensationRecordView resolveCompensation(Long compensationId, String note, UserContext operator, String requestId) {
        CompensationRecordEntity entity = compensationRecordMapper.selectById(compensationId);
        if (entity == null || !Objects.equals(entity.getIsDeleted(), 0)) {
            throw new IllegalArgumentException("补偿记录不存在");
        }
        entity.setStatus(COMPENSATION_STATUS_RESOLVED);
        entity.setResolutionNote(note);
        entity.setResolvedAt(LocalDateTime.now(clock));
        entity.setResolvedBy(operator.userId());
        entity.setUpdatedBy(operator.userId());
        compensationRecordMapper.updateById(entity);
        recordAudit(
                BIZ_TYPE_COMPENSATION,
                String.valueOf(entity.getId()),
                "COMPENSATION_RESOLVED",
                operator.userId(),
                requestId,
                Map.of(
                        "bizKey", entity.getBizKey(),
                        "note", note
                )
        );
        return toView(compensationRecordMapper.selectById(compensationId));
    }

    @Transactional
    public void processTask(ExportGeneratePayload payload) {
        ExportTaskEntity entity = exportTaskMapper.findByIdActive(payload.taskId());
        if (entity == null) {
            return;
        }
        if (EXPORT_TASK_STATUS_SUCCESS.equals(entity.getStatus())
                || EXPORT_TASK_STATUS_PROCESSING.equals(entity.getStatus())) {
            return;
        }
        entity.setStatus(EXPORT_TASK_STATUS_PROCESSING);
        entity.setFailReason(null);
        entity.setUpdatedBy(entity.getOperatorId());
        exportTaskMapper.updateById(entity);
        recordAudit(
                BIZ_TYPE_EXPORT_TASK,
                String.valueOf(entity.getId()),
                "EXPORT_TASK_PROCESSING",
                entity.getOperatorId(),
                null,
                Map.of("activityId", entity.getActivityId())
        );

        try {
            Files.createDirectories(exportDirectory);
            List<ExportRow> rows = queryRows(entity.getActivityId(), parseFilters(entity.getFiltersJson()));
            String fileName = buildFileName(entity);
            Path filePath = exportDirectory.resolve(fileName).normalize();
            if (!filePath.startsWith(exportDirectory)) {
                throw new IllegalStateException("导出文件路径非法");
            }
            writeFile(filePath, entity.getFormat(), rows);
            entity.setStatus(EXPORT_TASK_STATUS_SUCCESS);
            entity.setFileUrl("/api/exports/files/" + fileName);
            entity.setFailReason(null);
            entity.setUpdatedBy(entity.getOperatorId());
            exportTaskMapper.updateById(entity);
            recordAudit(
                    BIZ_TYPE_EXPORT_TASK,
                    String.valueOf(entity.getId()),
                    "EXPORT_TASK_SUCCEEDED",
                    entity.getOperatorId(),
                    null,
                    Map.of(
                            "fileUrl", entity.getFileUrl(),
                            "rowCount", rows.size()
                    )
            );
        } catch (Exception exception) {
            String reason = "导出文件生成失败";
            entity.setStatus(EXPORT_TASK_STATUS_FAILED);
            entity.setFailReason(reason);
            entity.setUpdatedBy(entity.getOperatorId());
            exportTaskMapper.updateById(entity);
            recordAudit(
                    BIZ_TYPE_EXPORT_TASK,
                    String.valueOf(entity.getId()),
                    "EXPORT_TASK_FAILED",
                    entity.getOperatorId(),
                    null,
                    Map.of("reason", reason)
            );
            publishDeadLetter(entity, reason);
        }
    }

    @Transactional
    public void recordDeadLetter(ExportDeadLetterPayload payload) {
        CompensationRecordEntity existing = compensationRecordMapper.findLatestByBizKeyAndSourceEvent(
                payload.bizKey(),
                "export.generate.dead"
        );
        if (existing != null && Objects.equals(existing.getIsDeleted(), 0)) {
            return;
        }

        CompensationRecordEntity entity = new CompensationRecordEntity();
        entity.setBizType(BIZ_TYPE_EXPORT_TASK);
        entity.setBizKey(payload.bizKey());
        entity.setSourceEvent("export.generate.dead");
        entity.setStatus(COMPENSATION_STATUS_PENDING);
        entity.setReason(payload.reason());
        entity.setPayloadJson(toJson(Map.of("taskId", payload.taskId(), "reason", payload.reason())));
        entity.setCreatedBy(0L);
        entity.setUpdatedBy(0L);
        entity.setIsDeleted(0);
        compensationRecordMapper.insert(entity);

        recordAudit(
                BIZ_TYPE_COMPENSATION,
                String.valueOf(entity.getId()),
                "COMPENSATION_RECORDED",
                0L,
                null,
                Map.of(
                        "bizKey", payload.bizKey(),
                        "reason", payload.reason()
                )
        );
    }

    public ResponseEntity<Resource> downloadFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("导出文件不存在");
        }
        Path filePath = exportDirectory.resolve(fileName).normalize();
        if (!filePath.startsWith(exportDirectory) || !Files.exists(filePath)) {
            throw new IllegalArgumentException("导出文件不存在");
        }
        Resource resource = toResource(filePath);
        MediaType mediaType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    private void publishExportGenerate(Long taskId) {
        DomainEvent<Map<String, Object>> event = DomainEvent.create(
                EXPORT_GENERATE_ROUTING_KEY,
                "export-task:" + taskId,
                Map.of("taskId", taskId),
                clock
        );
        rabbitTemplate.convertAndSend(EXPORT_EXCHANGE, EXPORT_GENERATE_ROUTING_KEY, event);
    }

    private void publishDeadLetter(ExportTaskEntity entity, String reason) {
        DomainEvent<Map<String, Object>> event = DomainEvent.create(
                EXPORT_DEAD_ROUTING_KEY,
                "export-task:" + entity.getId(),
                Map.of("taskId", entity.getId(), "reason", reason),
                clock
        );
        rabbitTemplate.convertAndSend(EXPORT_EXCHANGE, EXPORT_DEAD_ROUTING_KEY, event);
    }

    private ActivityProductEntity loadActivity(Long activityId) {
        return activityProductMapper.selectOne(new LambdaQueryWrapper<ActivityProductEntity>()
                .eq(ActivityProductEntity::getId, activityId)
                .eq(ActivityProductEntity::getIsDeleted, 0)
                .last("limit 1"));
    }

    private String normalizeFormat(String rawFormat) {
        if (rawFormat == null || rawFormat.isBlank()) {
            throw new IllegalArgumentException("导出格式不能为空");
        }
        String normalized = rawFormat.trim().toUpperCase();
        if (!"CSV".equals(normalized) && !"XLSX".equals(normalized)) {
            throw new IllegalArgumentException("仅支持 CSV 或 XLSX 导出");
        }
        return normalized;
    }

    private Map<String, Object> parseFilters(String filtersJson) {
        if (filtersJson == null || filtersJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(filtersJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("导出筛选条件解析失败", exception);
        }
    }

    private void writeFile(Path filePath, String format, List<ExportRow> rows) throws IOException {
        if ("CSV".equals(format)) {
            writeCsv(filePath, rows);
            return;
        }
        if ("XLSX".equals(format)) {
            EasyExcel.write(filePath.toFile(), ExportExcelRow.class)
                    .sheet("orders")
                    .doWrite(rows.stream().map(ExportExcelRow::from).toList());
            return;
        }
        throw new IllegalArgumentException("不支持的导出格式");
    }

    private void writeCsv(Path filePath, List<ExportRow> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write("orderNo,userId,activityId,orderStatus,payStatus,codeStatus,priceAmount,code,updatedAt");
            writer.newLine();
            for (ExportRow row : rows) {
                writer.write(String.join(",",
                        csv(row.orderNo()),
                        csv(String.valueOf(row.userId())),
                        csv(String.valueOf(row.activityId())),
                        csv(row.orderStatus()),
                        csv(row.payStatus()),
                        csv(row.codeStatus()),
                        csv(row.priceAmount().toPlainString()),
                        csv(Optional.ofNullable(row.code()).orElse("")),
                        csv(row.updatedAt() == null ? "" : row.updatedAt().toString())
                ));
                writer.newLine();
            }
        }
    }

    private List<ExportRow> queryRows(Long activityId, Map<String, Object> filters) {
        StringBuilder sql = new StringBuilder("""
                select o.order_no,
                       o.user_id,
                       o.activity_id,
                       o.order_status,
                       o.pay_status,
                       o.code_status,
                       o.price_amount,
                       o.updated_at,
                       rc.code
                from order_record o
                left join redeem_code rc
                  on rc.assigned_order_id = o.id
                 and rc.is_deleted = 0
                where o.activity_id = ?
                  and o.is_deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(activityId);
        appendFilter(sql, args, "o.pay_status", filters.get("payStatus"));
        appendFilter(sql, args, "o.order_status", filters.get("orderStatus"));
        appendFilter(sql, args, "o.code_status", filters.get("codeStatus"));
        appendFilter(sql, args, "o.user_id", filters.get("userId"));
        sql.append(" order by o.id asc");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> new ExportRow(
                rs.getString("order_no"),
                rs.getLong("user_id"),
                rs.getLong("activity_id"),
                rs.getString("order_status"),
                rs.getString("pay_status"),
                rs.getString("code_status"),
                rs.getBigDecimal("price_amount"),
                rs.getString("code"),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime()
        ));
    }

    private void appendFilter(StringBuilder sql, List<Object> args, String column, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String stringValue && stringValue.isBlank()) {
            return;
        }
        sql.append(" and ").append(column).append(" = ?");
        args.add(value);
    }

    private String buildFileName(ExportTaskEntity entity) {
        String suffix = "CSV".equals(entity.getFormat()) ? ".csv" : ".xlsx";
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now(clock));
        return "activity-%d-export-%d-%s%s".formatted(entity.getActivityId(), entity.getId(), timestamp, suffix);
    }

    private void recordAudit(
            String bizType,
            String bizKey,
            String operation,
            Long operatorId,
            String requestId,
            Map<String, Object> detail
    ) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setBizType(bizType);
        entity.setBizKey(bizKey);
        entity.setOperation(operation);
        entity.setOperatorId(operatorId);
        entity.setRequestId(requestId);
        entity.setDetailJson(toJson(detail));
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        entity.setIsDeleted(0);
        auditLogMapper.insert(entity);
    }

    private ExportTaskView toView(ExportTaskEntity entity) {
        return new ExportTaskView(
                entity.getId(),
                entity.getActivityId(),
                entity.getOperatorId(),
                entity.getFormat(),
                parseFilters(entity.getFiltersJson()),
                entity.getStatus(),
                entity.getFileUrl(),
                entity.getFailReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private CompensationRecordView toView(CompensationRecordEntity entity) {
        return new CompensationRecordView(
                entity.getId(),
                entity.getBizType(),
                entity.getBizKey(),
                entity.getSourceEvent(),
                entity.getStatus(),
                entity.getReason(),
                entity.getResolutionNote(),
                entity.getResolvedAt(),
                entity.getCreatedAt()
        );
    }

    private Resource toResource(Path filePath) {
        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException exception) {
            throw new IllegalStateException("导出文件路径非法", exception);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON 序列化失败", exception);
        }
    }

    private String csv(String value) {
        String actual = value == null ? "" : value;
        if (!actual.contains(",") && !actual.contains("\"") && !actual.contains("\n")) {
            return actual;
        }
        return "\"" + actual.replace("\"", "\"\"") + "\"";
    }

    public record ExportTaskCreateCommand(
            Long activityId,
            String format,
            Map<String, Object> filters,
            Long operatorId,
            String requestId
    ) {
    }

    public record ExportTaskView(
            Long id,
            Long activityId,
            Long operatorId,
            String format,
            Map<String, Object> filters,
            String status,
            String fileUrl,
            String failReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record CompensationRecordView(
            Long id,
            String bizType,
            String bizKey,
            String sourceEvent,
            String status,
            String reason,
            String resolutionNote,
            LocalDateTime resolvedAt,
            LocalDateTime createdAt
    ) {
    }

    private record ExportRow(
            String orderNo,
            Long userId,
            Long activityId,
            String orderStatus,
            String payStatus,
            String codeStatus,
            BigDecimal priceAmount,
            String code,
            LocalDateTime updatedAt
    ) {
    }

    private record ExportExcelRow(
            @ExcelProperty("订单号") String orderNo,
            @ExcelProperty("用户ID") Long userId,
            @ExcelProperty("活动ID") Long activityId,
            @ExcelProperty("订单状态") String orderStatus,
            @ExcelProperty("支付状态") String payStatus,
            @ExcelProperty("发码状态") String codeStatus,
            @ExcelProperty("订单金额") BigDecimal priceAmount,
            @ExcelProperty("兑换码") String code,
            @ExcelProperty("更新时间") String updatedAt
    ) {
        private static ExportExcelRow from(ExportRow row) {
            return new ExportExcelRow(
                    row.orderNo(),
                    row.userId(),
                    row.activityId(),
                    row.orderStatus(),
                    row.payStatus(),
                    row.codeStatus(),
                    row.priceAmount(),
                    row.code(),
                    row.updatedAt() == null ? null : row.updatedAt().toString()
            );
        }
    }
}
