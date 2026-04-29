package com.flashsale.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.activity.domain.ActivityEntity;
import com.flashsale.activity.domain.CodeSourceMode;
import com.flashsale.activity.domain.PublishStatus;
import com.flashsale.activity.domain.RedeemCodeEntity;
import com.flashsale.activity.domain.RedeemCodeImportBatchEntity;
import com.flashsale.activity.domain.RedeemCodeImportFailureEntity;
import com.flashsale.activity.mapper.ActivityMapper;
import com.flashsale.activity.mapper.RedeemCodeImportBatchMapper;
import com.flashsale.activity.mapper.RedeemCodeImportFailureMapper;
import com.flashsale.activity.mapper.RedeemCodeMapper;
import com.flashsale.activity.web.dto.RedeemCodeImportBatchDetailResponse;
import com.flashsale.activity.web.dto.RedeemCodeImportBatchSummaryResponse;
import com.flashsale.activity.web.dto.RedeemCodeImportFailureResponse;
import com.flashsale.common.security.context.UserContext;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 兑换码导入服务
 * 负责处理兑换码的批量导入、验证、解析以及导入批次记录的查询
 */
@Service
public class RedeemCodeImportService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{6,128}$");

    private static final Set<String> HEADER_NAMES = Set.of("code", "redeem_code", "兑换码");

    private static final DateTimeFormatter BATCH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ActivityMapper activityMapper;

    private final RedeemCodeMapper redeemCodeMapper;

    private final RedeemCodeImportBatchMapper redeemCodeImportBatchMapper;

    private final RedeemCodeImportFailureMapper redeemCodeImportFailureMapper;

    public RedeemCodeImportService(
            ActivityMapper activityMapper,
            RedeemCodeMapper redeemCodeMapper,
            RedeemCodeImportBatchMapper redeemCodeImportBatchMapper,
            RedeemCodeImportFailureMapper redeemCodeImportFailureMapper
    ) {
        this.activityMapper = activityMapper;
        this.redeemCodeMapper = redeemCodeMapper;
        this.redeemCodeImportBatchMapper = redeemCodeImportBatchMapper;
        this.redeemCodeImportFailureMapper = redeemCodeImportFailureMapper;
    }

    /**
     * 导入兑换码
     * 解析上传的文件（CSV或XLSX），校验兑换码格式及去重，并将成功和失败的记录保存到数据库中，最后生成导入批次记录。
     *
     * @param activityId  活动ID
     * @param file        包含兑换码的上传文件
     * @param userContext 当前操作用户上下文
     * @return 导入批次的详细结果响应
     */
    @Transactional
    public RedeemCodeImportBatchDetailResponse importCodes(Long activityId, MultipartFile file, UserContext userContext) {
        ActivityEntity activity = getRequiredActivity(activityId);
        validateImportActivity(activity);
        validateFile(file);

        List<ParsedCodeRow> parsedRows = parseRows(file);
        if (parsedRows.isEmpty()) {
            throw new IllegalArgumentException("导入文件中没有可解析的数据");
        }

        Set<String> existingCodes = findExistingCodes(parsedRows);
        // seenCodes 用于记录当前文件中已经处理过的兑换码，避免同一文件内的重复
        Set<String> seenCodes = new HashSet<>();
        List<RedeemCodeEntity> successCodes = new ArrayList<>();
        // 记录导入失败的行，包含行号、原始内容和失败原因
        List<RedeemCodeImportFailureEntity> failures = new ArrayList<>();
        String batchNo = generateBatchNo(activityId);
        Long operatorId = operatorId(userContext);

        // 逐行校验解析结果，记录成功和失败的兑换码
        for (ParsedCodeRow parsedRow : parsedRows) {
            String normalizedCode = normalize(parsedRow.rawCode());
            if (normalizedCode.isBlank()) {
                failures.add(failure(activityId, batchNo, parsedRow.lineNumber(), "", "EMPTY_CODE", operatorId));
                continue;
            }
            if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
                failures.add(failure(activityId, batchNo, parsedRow.lineNumber(), normalizedCode, "INVALID_FORMAT", operatorId));
                continue;
            }
            // 检查是否在当前文件中重复
            if (!seenCodes.add(normalizedCode)) {
                failures.add(failure(activityId, batchNo, parsedRow.lineNumber(), normalizedCode, "DUPLICATE_IN_FILE", operatorId));
                continue;
            }
            if (existingCodes.contains(normalizedCode)) {
                failures.add(failure(activityId, batchNo, parsedRow.lineNumber(), normalizedCode, "DUPLICATE_IN_SYSTEM", operatorId));
                continue;
            }
            successCodes.add(successCode(activityId, batchNo, normalizedCode, operatorId));
        }

        for (RedeemCodeEntity successCode : successCodes) {
            redeemCodeMapper.insert(successCode);
        }
        for (RedeemCodeImportFailureEntity failure : failures) {
            redeemCodeImportFailureMapper.insert(failure);
        }

        // 创建导入批次记录，包含统计信息和文件元数据
        RedeemCodeImportBatchEntity batch = new RedeemCodeImportBatchEntity();
        batch.setActivityId(activityId);
        batch.setBatchNo(batchNo);
        batch.setFileName(file.getOriginalFilename());
        batch.setTotalCount(parsedRows.size());
        batch.setSuccessCount(successCodes.size());
        batch.setFailedCount(failures.size());
        batch.setCreatedBy(operatorId);
        batch.setUpdatedBy(operatorId);
        batch.setIsDeleted(0);
        redeemCodeImportBatchMapper.insert(batch);

        // 
        return RedeemCodeImportBatchDetailResponse.fromEntity(
                batch,
                failures.stream().map(RedeemCodeImportFailureResponse::fromEntity).toList()
        );
    }

    /**
     * 获取活动的导入批次列表
     *
     * @param activityId 活动ID
     * @return 导入批次摘要信息的列表
     */
    public List<RedeemCodeImportBatchSummaryResponse> listBatches(Long activityId) {
        getRequiredActivity(activityId);
        return redeemCodeImportBatchMapper.selectList(
                        new LambdaQueryWrapper<RedeemCodeImportBatchEntity>()
                                .eq(RedeemCodeImportBatchEntity::getActivityId, activityId)
                                .eq(RedeemCodeImportBatchEntity::getIsDeleted, 0)
                                .orderByDesc(RedeemCodeImportBatchEntity::getId)
                ).stream()
                .map(RedeemCodeImportBatchSummaryResponse::fromEntity)
                .toList();
    }

    /**
     * 获取指定导入批次的详细信息（包含失败记录）
     *
     * @param activityId 活动ID
     * @param batchNo    导入批次号
     * @return 批次详细信息和失败记录明细
     */
    public RedeemCodeImportBatchDetailResponse getBatchDetail(Long activityId, String batchNo) {
        getRequiredActivity(activityId);
        RedeemCodeImportBatchEntity batch = redeemCodeImportBatchMapper.selectOne(
                new LambdaQueryWrapper<RedeemCodeImportBatchEntity>()
                        .eq(RedeemCodeImportBatchEntity::getActivityId, activityId)
                        .eq(RedeemCodeImportBatchEntity::getBatchNo, batchNo)
                        .eq(RedeemCodeImportBatchEntity::getIsDeleted, 0)
                        .last("limit 1")
        );
        if (batch == null) {
            throw new IllegalArgumentException("导入批次不存在");
        }
        List<RedeemCodeImportFailureResponse> failures = redeemCodeImportFailureMapper.selectList(
                        new LambdaQueryWrapper<RedeemCodeImportFailureEntity>()
                                .eq(RedeemCodeImportFailureEntity::getActivityId, activityId)
                                .eq(RedeemCodeImportFailureEntity::getBatchNo, batchNo)
                                .eq(RedeemCodeImportFailureEntity::getIsDeleted, 0)
                                .orderByAsc(RedeemCodeImportFailureEntity::getLineNo)
                ).stream()
                .map(RedeemCodeImportFailureResponse::fromEntity)
                .toList();
        return RedeemCodeImportBatchDetailResponse.fromEntity(batch, failures);
    }

    private Set<String> findExistingCodes(List<ParsedCodeRow> parsedRows) {
        Set<String> candidateCodes = new HashSet<>();
        for (ParsedCodeRow parsedRow : parsedRows) {
            String normalizedCode = normalize(parsedRow.rawCode());
            // 仅对非空且格式正确的兑换码进行数据库查询
            if (!normalizedCode.isBlank() && CODE_PATTERN.matcher(normalizedCode).matches()) {
                candidateCodes.add(normalizedCode);
            }
        }
        if (candidateCodes.isEmpty()) {
            return Set.of();
        }
        // 从数据库中查询已存在的兑换码
        return redeemCodeMapper.selectList(
                        new LambdaQueryWrapper<RedeemCodeEntity>()
                                .in(RedeemCodeEntity::getCode, candidateCodes)
                                .eq(RedeemCodeEntity::getIsDeleted, 0)
                ).stream()
                .map(RedeemCodeEntity::getCode)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    private List<ParsedCodeRow> parseRows(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("导入文件名不能为空");
        }
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        try {
            if (lowerFileName.endsWith(".csv")) {
                return parseCsv(file);
            }
            if (lowerFileName.endsWith(".xlsx")) {
                return parseXlsx(file);
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("导入文件解析失败");
        }
        throw new IllegalArgumentException("仅支持 csv 或 xlsx 文件导入");
    }

    private List<ParsedCodeRow> parseCsv(MultipartFile file) throws IOException {
        List<ParsedCodeRow> rows = new ArrayList<>();

        // file.getInputStream()：获取上传文件的输入流
        // InputStreamReader(..., StandardCharsets.UTF_8)：将字节流按 UTF-8 解码为字符流。避免了平台默认编码导致的乱码
        // BufferedReader：带缓冲的读取器，提高了按行读取的效率。
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String rawCode = firstColumn(lineNumber == 1 ? stripBom(line) : line);
                if (lineNumber == 1 && isHeader(rawCode)) {
                    continue;
                }
                rows.add(new ParsedCodeRow(lineNumber, rawCode));
            }
        }
        return rows;
    }

    private List<ParsedCodeRow> parseXlsx(MultipartFile file) throws IOException {
        List<ParsedCodeRow> rows = new ArrayList<>();

        // 使用 Apache POI 解析 XLSX 文件，支持更复杂的表格结构和格式，第一行同样支持表头识别
        DataFormatter dataFormatter = new DataFormatter();

        // XSSFWorkbook 直接从输入流创建，无需将整个文件加载到内存中
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {

            // getNumberOfSheets()获取工作表数量，如果没有工作表则直接返回空列表
            if (workbook.getNumberOfSheets() == 0) {
                return rows;
            }

            // getSheetAt(0)获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            // getFirstRowNum()返回工作表中物理存在的第一行索引，通常为0
            int firstRowIndex = sheet.getFirstRowNum();
            // getLastRowNum()返回最后一行索引
            int lastRowIndex = sheet.getLastRowNum();

            for (int rowIndex = firstRowIndex; rowIndex <= lastRowIndex; rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                // dataFormatter.formatCellValue(...)将单元格内容格式化为字符串
                String rawCode = row == null ? "" : dataFormatter.formatCellValue(row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));
                // 第一行如果是表头则跳过
                if (rowIndex == firstRowIndex && isHeader(rawCode)) {
                    continue;
                }
                // 
                rows.add(new ParsedCodeRow(rowIndex + 1, rawCode));
            }
        }
        return rows;
    }

    // 从CSV行中提取第一列作为兑换码，支持逗号分隔，允许空列但不允许行完全为空
    private String firstColumn(String line) {
        String[] columns = line.split(",", -1);
        return columns.length == 0 ? "" : columns[0];
    }

    // 删除字符串开头的 BOM 字符（如果存在），避免解析时出现乱码
    private String stripBom(String value) {
        if (!value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    // 判断第一行是否为表头（不区分大小写，允许前后有空白）
    private boolean isHeader(String rawCode) {
        return HEADER_NAMES.contains(normalize(rawCode).toLowerCase(Locale.ROOT));
    }

    // 字符串规范化，去除首尾空白，如果输入为null则返回空字符串
    private String normalize(String rawCode) {
        return rawCode == null ? "" : rawCode.trim();
    }

    private void validateImportActivity(ActivityEntity activity) {
        if (!PublishStatus.UNPUBLISHED.name().equals(activity.getPublishStatus())) {
            throw new IllegalArgumentException("仅未发布活动允许导入兑换码");
        }
        if (!CodeSourceMode.THIRD_PARTY_IMPORTED.name().equals(activity.getCodeSourceMode())) {
            throw new IllegalArgumentException("仅第三方导入模式活动允许导入兑换码");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("导入文件不能为空");
        }
    }

    private ActivityEntity getRequiredActivity(Long activityId) {
        ActivityEntity activity = activityMapper.selectOne(
                new LambdaQueryWrapper<ActivityEntity>()
                        .eq(ActivityEntity::getId, activityId)
                        .eq(ActivityEntity::getIsDeleted, 0)
                        .last("limit 1")
        );
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        return activity;
    }

    private RedeemCodeEntity successCode(Long activityId, String batchNo, String code, Long operatorId) {
        RedeemCodeEntity entity = new RedeemCodeEntity();
        entity.setActivityId(activityId);
        entity.setCode(code);
        entity.setSourceType(CodeSourceMode.THIRD_PARTY_IMPORTED.name());
        entity.setBatchNo(batchNo);
        entity.setStatus("AVAILABLE");
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        entity.setIsDeleted(0);
        return entity;
    }

    private RedeemCodeImportFailureEntity failure(
            Long activityId,
            String batchNo,
            Integer lineNumber,
            String rawCode,
            String reason,
            Long operatorId
    ) {
        RedeemCodeImportFailureEntity entity = new RedeemCodeImportFailureEntity();
        entity.setActivityId(activityId);
        entity.setBatchNo(batchNo);
        entity.setLineNo(lineNumber);
        entity.setRawCode(rawCode);
        entity.setFailureReason(reason);
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        entity.setIsDeleted(0);
        return entity;
    }

    // 生成唯一的导入批次号，格式为 "IMP-{activityId}-{timestamp}-{random}"
    private String generateBatchNo(Long activityId) {
        return "IMP-" + activityId + "-" + LocalDateTime.now().format(BATCH_TIME_FORMATTER)
                + "-" + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    // 从用户上下文中提取操作人ID
    private Long operatorId(UserContext userContext) {
        return userContext == null ? null : userContext.userId();
    }

    private record ParsedCodeRow(int lineNumber, String rawCode) {
    }
}
