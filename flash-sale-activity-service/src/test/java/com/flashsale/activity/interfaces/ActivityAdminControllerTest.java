package com.flashsale.activity.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.activity.FlashSaleActivityApplication;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.common.security.context.UserContext;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FlashSaleActivityApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActivityAdminControllerTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private ZSetOperations<String, String> zSetOperations;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from redeem_code_import_fail_detail");
        jdbcTemplate.update("delete from redeem_code_import_batch");
        jdbcTemplate.update("delete from redeem_code");
        jdbcTemplate.update("delete from activity_product");

        valueOperations = Mockito.mock(ValueOperations.class);
        hashOperations = Mockito.mock(HashOperations.class);
        zSetOperations = Mockito.mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void createImmediateActivityPublishesDirectlyWithoutPublishTime() throws Exception {
        mockMvc.perform(admin(post("/api/activities"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activityJson(
                                "新人礼品卡秒杀",
                                "SYSTEM_GENERATED",
                                "IMMEDIATE",
                                null,
                                nowPlusMinutes(20),
                                nowPlusMinutes(40),
                                50,
                                BigDecimal.ZERO,
                                false
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.title").value("新人礼品卡秒杀"))
                .andExpect(jsonPath("$.data.availableStock").value(50))
                .andExpect(jsonPath("$.data.publishStatus").value("PUBLISHED"));

        Map<String, Object> row = jdbcTemplate.queryForMap("select * from activity_product");
        assertThat(row.get("title")).isEqualTo("新人礼品卡秒杀");
        assertThat(row.get("available_stock")).isEqualTo(50);
        assertThat(row.get("publish_status")).isEqualTo("PUBLISHED");
        verify(valueOperations, times(1))
                .set(eq(RedisKeys.seckillStock(((Number) row.get("id")).longValue())), eq("50"), any());
        verify(hashOperations, times(1))
                .putAll(eq(RedisKeys.activityDetail(((Number) row.get("id")).longValue())), anyMap());
        verify(zSetOperations, times(1))
                .add(eq(RedisKeys.activityVisibleList()), eq(String.valueOf(((Number) row.get("id")).longValue())), anyDouble());
    }

    @Test
    void updateImmediateActivityPublishesDirectly() throws Exception {
        Long activityId = insertActivity("待更新活动", "SYSTEM_GENERATED", "IMMEDIATE", "UNPUBLISHED",
                nowPlusMinutes(5), nowPlusMinutes(20), nowPlusMinutes(60), 30, BigDecimal.ZERO, false);

        mockMvc.perform(admin(put("/api/activities/{activityId}", activityId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activityJson(
                                "已更新活动",
                                "SYSTEM_GENERATED",
                                "IMMEDIATE",
                                null,
                                nowPlusMinutes(25),
                                nowPlusMinutes(80),
                                80,
                                BigDecimal.ZERO,
                                false
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(activityId))
                .andExpect(jsonPath("$.data.title").value("已更新活动"))
                .andExpect(jsonPath("$.data.availableStock").value(80))
                .andExpect(jsonPath("$.data.publishStatus").value("PUBLISHED"));

        Map<String, Object> row = jdbcTemplate.queryForMap("select * from activity_product where id = ?", activityId);
        assertThat(row.get("title")).isEqualTo("已更新活动");
        assertThat(row.get("available_stock")).isEqualTo(80);
        assertThat(row.get("total_stock")).isEqualTo(80);
        assertThat(row.get("publish_status")).isEqualTo("PUBLISHED");
        verify(valueOperations, times(1))
                .set(eq(RedisKeys.seckillStock(activityId)), eq("80"), any());
        verify(hashOperations, times(1))
                .putAll(eq(RedisKeys.activityDetail(activityId)), anyMap());
        verify(zSetOperations, times(1))
                .add(eq(RedisKeys.activityVisibleList()), eq(String.valueOf(activityId)), anyDouble());
    }

    @Test
    void detailAndListReturnDerivedPhase() throws Exception {
        Long activityId = insertActivity("预告活动", "SYSTEM_GENERATED", "IMMEDIATE", "PUBLISHED",
                nowMinusMinutes(10), nowPlusMinutes(10), nowPlusMinutes(30), 10, BigDecimal.ZERO, false);

        mockMvc.perform(admin(get("/api/activities/{activityId}", activityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(activityId))
                .andExpect(jsonPath("$.data.phase").value("PREVIEW"));

        mockMvc.perform(admin(get("/api/activities")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].id").value(activityId))
                .andExpect(jsonPath("$.data[0].phase").value("PREVIEW"));
    }

    @Test
    void publishImmediateMarksPublishedAndWarmsUpCache() throws Exception {
        Long activityId = insertActivity("立即发布活动", "SYSTEM_GENERATED", "IMMEDIATE", "UNPUBLISHED",
                nowPlusMinutes(1), nowPlusMinutes(10), nowPlusMinutes(30), 12, BigDecimal.ZERO, false);

        mockMvc.perform(admin(post("/api/activities/{activityId}/publish", activityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.publishStatus").value("PUBLISHED"));

        assertThat(queryPublishStatus(activityId)).isEqualTo("PUBLISHED");
        verify(valueOperations, times(1))
                .set(eq(RedisKeys.seckillStock(activityId)), eq("12"), any());
        verify(hashOperations, times(1))
                .putAll(eq(RedisKeys.activityDetail(activityId)), anyMap());
        verify(zSetOperations, times(1))
                .add(eq(RedisKeys.activityVisibleList()), eq(String.valueOf(activityId)), anyDouble());
    }

    @Test
    void publishScheduledKeepsActivityUnpublishedUntilSchedulerRuns() throws Exception {
        Long activityId = insertActivity("定时发布活动", "SYSTEM_GENERATED", "SCHEDULED", "UNPUBLISHED",
                nowPlusMinutes(30), nowPlusMinutes(60), nowPlusMinutes(120), 18, BigDecimal.ZERO, false);

        mockMvc.perform(admin(post("/api/activities/{activityId}/publish", activityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.publishStatus").value("UNPUBLISHED"));

        assertThat(queryPublishStatus(activityId)).isEqualTo("UNPUBLISHED");
        verify(valueOperations, never()).set(any(), any(), any());
        verify(hashOperations, never()).putAll(any(), anyMap());
        verify(zSetOperations, never()).add(any(), any(), anyDouble());
    }

    @Test
    void advancePublishScheduledActivityPublishesImmediately() throws Exception {
        Long activityId = insertActivity("定时活动提前发布", "SYSTEM_GENERATED", "SCHEDULED", "UNPUBLISHED",
                nowPlusMinutes(30), nowPlusMinutes(60), nowPlusMinutes(120), 18, BigDecimal.ZERO, false);

        mockMvc.perform(admin(post("/api/activities/{activityId}/advance-publish", activityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.publishStatus").value("PUBLISHED"));

        assertThat(queryPublishStatus(activityId)).isEqualTo("PUBLISHED");
        assertThat(queryPublishTime(activityId)).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
        verify(valueOperations, times(1))
                .set(eq(RedisKeys.seckillStock(activityId)), eq("18"), any());
        verify(hashOperations, times(1))
                .putAll(eq(RedisKeys.activityDetail(activityId)), anyMap());
        verify(zSetOperations, times(1))
                .add(eq(RedisKeys.activityVisibleList()), eq(String.valueOf(activityId)), anyDouble());
    }

    @Test
    void createScheduledActivityWithoutPublishTimeIsRejected() throws Exception {
        mockMvc.perform(admin(post("/api/activities"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activityJson(
                                "定时活动缺少发布时间",
                                "SYSTEM_GENERATED",
                                "SCHEDULED",
                                null,
                                nowPlusMinutes(20),
                                nowPlusMinutes(40),
                                50,
                                BigDecimal.ZERO,
                                false
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("定时发布必须设置发布时间"));
    }

    @Test
    void offlineActivityMarksOfflineAndClearsCache() throws Exception {
        Long activityId = insertActivity("已发布活动", "SYSTEM_GENERATED", "IMMEDIATE", "PUBLISHED",
                nowMinusMinutes(5), nowPlusMinutes(10), nowPlusMinutes(40), 25, BigDecimal.ZERO, false);

        mockMvc.perform(admin(post("/api/activities/{activityId}/offline", activityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.publishStatus").value("OFFLINE"));

        assertThat(queryPublishStatus(activityId)).isEqualTo("OFFLINE");
        verify(stringRedisTemplate, times(1)).delete(RedisKeys.seckillStock(activityId));
        verify(stringRedisTemplate, times(1)).delete(RedisKeys.activityDetail(activityId));
        verify(zSetOperations, times(1))
                .remove(RedisKeys.activityVisibleList(), String.valueOf(activityId));
    }

    @Test
    void deleteDraftActivityMarksDeletedAndClearsCache() throws Exception {
        Long activityId = insertActivity("待删除草稿活动", "SYSTEM_GENERATED", "IMMEDIATE", "UNPUBLISHED",
                nowPlusMinutes(5), nowPlusMinutes(10), nowPlusMinutes(40), 15, BigDecimal.ZERO, false);

        mockMvc.perform(admin(delete("/api/activities/{activityId}", activityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        assertThat(queryDeletedFlag(activityId)).isEqualTo(1);
        verify(stringRedisTemplate, times(1)).delete(RedisKeys.seckillStock(activityId));
        verify(stringRedisTemplate, times(1)).delete(RedisKeys.activityDetail(activityId));
        verify(zSetOperations, times(1))
                .remove(RedisKeys.activityVisibleList(), String.valueOf(activityId));
    }

    @Test
    void deleteOfflineActivityRemovesItFromList() throws Exception {
        Long activityId = insertActivity("待删除下线活动", "SYSTEM_GENERATED", "IMMEDIATE", "OFFLINE",
                nowMinusMinutes(10), nowMinusMinutes(5), nowPlusMinutes(10), 20, BigDecimal.ZERO, false);

        mockMvc.perform(admin(delete("/api/activities/{activityId}", activityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        assertThat(queryDeletedFlag(activityId)).isEqualTo(1);

        mockMvc.perform(admin(get("/api/activities")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void deletePublishedActivityIsRejected() throws Exception {
        Long activityId = insertActivity("不可删除已发布活动", "SYSTEM_GENERATED", "IMMEDIATE", "PUBLISHED",
                nowMinusMinutes(5), nowPlusMinutes(10), nowPlusMinutes(40), 25, BigDecimal.ZERO, false);

        mockMvc.perform(admin(delete("/api/activities/{activityId}", activityId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("仅未发布或已下线活动允许删除"));
    }

    @Test
    void publishRejectsThirdPartyActivityWhenAvailableCodesAreInsufficient() throws Exception {
        Long activityId = insertActivity("第三方码活动", "THIRD_PARTY_IMPORTED", "IMMEDIATE", "UNPUBLISHED",
                nowPlusMinutes(1), nowPlusMinutes(10), nowPlusMinutes(30), 2, BigDecimal.ZERO, false);

        mockMvc.perform(admin(post("/api/activities/{activityId}/publish", activityId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("第三方兑换码可用数量不足"));
    }

    @Test
    void importCsvCreatesBatchAndFailureDetails() throws Exception {
        Long activityId = insertActivity("导入兑换码活动", "THIRD_PARTY_IMPORTED", "IMMEDIATE", "UNPUBLISHED",
                nowPlusMinutes(1), nowPlusMinutes(10), nowPlusMinutes(30), 2, BigDecimal.ZERO, false);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "codes.csv",
                "text/csv",
                """
                        code
                        CODE1001
                        CODE1001
                        BAD CODE
                        
                        CODE1002
                        """.getBytes()
        );

        String response = mockMvc.perform(admin(multipart("/api/activities/{activityId}/codes/import", activityId))
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.fileName").value("codes.csv"))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failedCount").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String batchNo = objectMapper.readTree(response).path("data").path("batchNo").asText();
        assertThat(batchNo).isNotBlank();

        assertThat(jdbcTemplate.queryForObject(
                "select count(1) from redeem_code where activity_id = ? and batch_no = ?",
                Integer.class,
                activityId,
                batchNo
        )).isEqualTo(2);

        assertThat(jdbcTemplate.queryForList(
                "select code, source_type, status from redeem_code where batch_no = ? order by code",
                batchNo
        )).containsExactly(
                Map.of("code", "CODE1001", "source_type", "THIRD_PARTY_IMPORTED", "status", "AVAILABLE"),
                Map.of("code", "CODE1002", "source_type", "THIRD_PARTY_IMPORTED", "status", "AVAILABLE")
        );

        assertThat(jdbcTemplate.queryForMap(
                "select total_count, success_count, failed_count from redeem_code_import_batch where batch_no = ?",
                batchNo
        )).containsEntry("total_count", 5)
                .containsEntry("success_count", 2)
                .containsEntry("failed_count", 3);

        assertThat(jdbcTemplate.queryForList(
                "select line_no, raw_code, failure_reason from redeem_code_import_fail_detail where batch_no = ? order by line_no",
                batchNo
        )).containsExactly(
                Map.of("line_no", 3, "raw_code", "CODE1001", "failure_reason", "DUPLICATE_IN_FILE"),
                Map.of("line_no", 4, "raw_code", "BAD CODE", "failure_reason", "INVALID_FORMAT"),
                Map.of("line_no", 5, "raw_code", "", "failure_reason", "EMPTY_CODE")
        );
    }

    @Test
    void importXlsxCreatesAvailableCodes() throws Exception {
        Long activityId = insertActivity("导入Excel兑换码活动", "THIRD_PARTY_IMPORTED", "IMMEDIATE", "UNPUBLISHED",
                nowPlusMinutes(1), nowPlusMinutes(10), nowPlusMinutes(30), 2, BigDecimal.ZERO, false);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "codes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxContent(List.of("code", "XLSX1001", "XLSX1002"))
        );

        mockMvc.perform(admin(multipart("/api/activities/{activityId}/codes/import", activityId))
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failedCount").value(0));

        assertThat(jdbcTemplate.queryForObject(
                "select count(1) from redeem_code where activity_id = ?",
                Integer.class,
                activityId
        )).isEqualTo(2);
    }

    @Test
    void importBatchEndpointsReturnSummariesAndFailures() throws Exception {
        Long activityId = insertActivity("查询导入记录活动", "THIRD_PARTY_IMPORTED", "IMMEDIATE", "UNPUBLISHED",
                nowPlusMinutes(1), nowPlusMinutes(10), nowPlusMinutes(30), 3, BigDecimal.ZERO, false);

        jdbcTemplate.update("""
                insert into redeem_code_import_batch (
                  activity_id, batch_no, file_name, total_count, success_count, failed_count, created_by, updated_by, is_deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, 0)
                """, activityId, "BATCH-OLD", "old.csv", 2, 2, 0, 1L, 1L);
        jdbcTemplate.update("""
                insert into redeem_code_import_batch (
                  activity_id, batch_no, file_name, total_count, success_count, failed_count, created_by, updated_by, is_deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, 0)
                """, activityId, "BATCH-NEW", "new.csv", 3, 2, 1, 1L, 1L);
        jdbcTemplate.update("""
                insert into redeem_code_import_fail_detail (
                  activity_id, batch_no, line_no, raw_code, failure_reason, created_by, updated_by, is_deleted
                ) values (?, ?, ?, ?, ?, ?, ?, 0)
                """, activityId, "BATCH-NEW", 2, "BAD CODE", "INVALID_FORMAT", 1L, 1L);

        mockMvc.perform(admin(get("/api/activities/{activityId}/codes/import-batches", activityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].batchNo").value("BATCH-NEW"))
                .andExpect(jsonPath("$.data[0].failedCount").value(1))
                .andExpect(jsonPath("$.data[1].batchNo").value("BATCH-OLD"));

        mockMvc.perform(admin(get("/api/activities/{activityId}/codes/import-batches/{batchNo}", activityId, "BATCH-NEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchNo").value("BATCH-NEW"))
                .andExpect(jsonPath("$.data.failures[0].lineNumber").value(2))
                .andExpect(jsonPath("$.data.failures[0].rawCode").value("BAD CODE"))
                .andExpect(jsonPath("$.data.failures[0].reason").value("INVALID_FORMAT"));
    }

    @Test
    void importRejectsPublishedOrNonThirdPartyActivities() throws Exception {
        Long publishedActivityId = insertActivity("已发布第三方活动", "THIRD_PARTY_IMPORTED", "IMMEDIATE", "PUBLISHED",
                nowMinusMinutes(1), nowPlusMinutes(10), nowPlusMinutes(30), 2, BigDecimal.ZERO, false);
        Long systemGeneratedActivityId = insertActivity("系统发码活动", "SYSTEM_GENERATED", "IMMEDIATE", "UNPUBLISHED",
                nowPlusMinutes(1), nowPlusMinutes(10), nowPlusMinutes(30), 2, BigDecimal.ZERO, false);
        MockMultipartFile file = new MockMultipartFile("file", "codes.csv", "text/csv", "code\nCODE1001\n".getBytes());

        mockMvc.perform(admin(multipart("/api/activities/{activityId}/codes/import", publishedActivityId))
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("仅未发布活动允许导入兑换码"));

        mockMvc.perform(admin(multipart("/api/activities/{activityId}/codes/import", systemGeneratedActivityId))
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("仅第三方导入模式活动允许导入兑换码"));
    }

    private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder builder) {
        return builder
                .header("X-Request-Id", "REQ-ACTIVITY-001")
                .header(UserContext.USER_ID_HEADER, 1L)
                .header(UserContext.USERNAME_HEADER, "publisher")
                .header(UserContext.ROLE_HEADER, "PUBLISHER");
    }

    private MockMultipartHttpServletRequestBuilder admin(MockMultipartHttpServletRequestBuilder builder) {
        builder.header("X-Request-Id", "REQ-ACTIVITY-001");
        builder.header(UserContext.USER_ID_HEADER, 1L);
        builder.header(UserContext.USERNAME_HEADER, "publisher");
        builder.header(UserContext.ROLE_HEADER, "PUBLISHER");
        return builder;
    }

    private Long insertActivity(
            String title,
            String codeSourceMode,
            String publishMode,
            String publishStatus,
            LocalDateTime publishTime,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int stock,
            BigDecimal priceAmount,
            boolean needPayment
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
                priceAmount,
                needPayment ? 1 : 0,
                "SINGLE",
                1,
                codeSourceMode,
                publishMode,
                publishStatus,
                publishTime,
                startTime,
                endTime
        );
        return jdbcTemplate.queryForObject("select max(id) from activity_product", Long.class);
    }

    private String activityJson(
            String title,
            String codeSourceMode,
            String publishMode,
            LocalDateTime publishTime,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int totalStock,
            BigDecimal priceAmount,
            boolean needPayment
    ) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", title + "描述");
        payload.put("coverUrl", "https://example.com/" + title + ".png");
        payload.put("totalStock", totalStock);
        payload.put("priceAmount", priceAmount);
        payload.put("needPayment", needPayment);
        payload.put("purchaseLimitType", "SINGLE");
        payload.put("purchaseLimitCount", 1);
        payload.put("codeSourceMode", codeSourceMode);
        payload.put("publishMode", publishMode);
        if (publishTime != null) {
            payload.put("publishTime", publishTime.format(FORMATTER));
        }
        payload.put("startTime", startTime.format(FORMATTER));
        payload.put("endTime", endTime.format(FORMATTER));
        return objectMapper.writeValueAsString(payload);
    }

    private String queryPublishStatus(Long activityId) {
        return jdbcTemplate.queryForObject(
                "select publish_status from activity_product where id = ?",
                String.class,
                activityId
        );
    }

    private Integer queryDeletedFlag(Long activityId) {
        return jdbcTemplate.queryForObject(
                "select is_deleted from activity_product where id = ?",
                Integer.class,
                activityId
        );
    }

    private LocalDateTime queryPublishTime(Long activityId) {
        return jdbcTemplate.queryForObject(
                "select publish_time from activity_product where id = ?",
                LocalDateTime.class,
                activityId
        );
    }

    private LocalDateTime nowPlusMinutes(long minutes) {
        return LocalDateTime.now().plusMinutes(minutes);
    }

    private LocalDateTime nowMinusMinutes(long minutes) {
        return LocalDateTime.now().minusMinutes(minutes);
    }

    private byte[] xlsxContent(List<String> values) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("codes");
            for (int index = 0; index < values.size(); index++) {
                sheet.createRow(index).createCell(0).setCellValue(values.get(index));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
