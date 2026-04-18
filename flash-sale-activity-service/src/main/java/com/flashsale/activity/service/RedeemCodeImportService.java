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
        Set<String> seenCodes = new HashSet<>();
        List<RedeemCodeEntity> successCodes = new ArrayList<>();
        List<RedeemCodeImportFailureEntity> failures = new ArrayList<>();
        String batchNo = generateBatchNo(activityId);
        Long operatorId = operatorId(userContext);

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

        return RedeemCodeImportBatchDetailResponse.fromEntity(
                batch,
                failures.stream().map(RedeemCodeImportFailureResponse::fromEntity).toList()
        );
    }

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
            if (!normalizedCode.isBlank() && CODE_PATTERN.matcher(normalizedCode).matches()) {
                candidateCodes.add(normalizedCode);
            }
        }
        if (candidateCodes.isEmpty()) {
            return Set.of();
        }
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
        DataFormatter dataFormatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                return rows;
            }
            Sheet sheet = workbook.getSheetAt(0);
            int firstRowIndex = sheet.getFirstRowNum();
            int lastRowIndex = sheet.getLastRowNum();
            for (int rowIndex = firstRowIndex; rowIndex <= lastRowIndex; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                String rawCode = row == null ? "" : dataFormatter.formatCellValue(row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));
                if (rowIndex == firstRowIndex && isHeader(rawCode)) {
                    continue;
                }
                rows.add(new ParsedCodeRow(rowIndex + 1, rawCode));
            }
        }
        return rows;
    }

    private String firstColumn(String line) {
        String[] columns = line.split(",", -1);
        return columns.length == 0 ? "" : columns[0];
    }

    private String stripBom(String value) {
        if (!value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private boolean isHeader(String rawCode) {
        return HEADER_NAMES.contains(normalize(rawCode).toLowerCase(Locale.ROOT));
    }

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

    private String generateBatchNo(Long activityId) {
        return "IMP-" + activityId + "-" + LocalDateTime.now().format(BATCH_TIME_FORMATTER)
                + "-" + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private Long operatorId(UserContext userContext) {
        return userContext == null ? null : userContext.userId();
    }

    private record ParsedCodeRow(int lineNumber, String rawCode) {
    }
}
