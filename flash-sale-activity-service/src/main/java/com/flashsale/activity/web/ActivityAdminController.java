package com.flashsale.activity.web;

import com.flashsale.activity.service.ActivityService;
import com.flashsale.activity.service.RedeemCodeImportService;
import com.flashsale.activity.web.dto.ActivityCreateRequest;
import com.flashsale.activity.web.dto.ActivityDetailResponse;
import com.flashsale.activity.web.dto.ActivitySummaryResponse;
import com.flashsale.activity.web.dto.ActivityUpdateRequest;
import com.flashsale.activity.web.dto.RedeemCodeImportBatchDetailResponse;
import com.flashsale.activity.web.dto.RedeemCodeImportBatchSummaryResponse;
import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.auth.RequireRole;
import com.flashsale.common.security.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityAdminController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final ActivityService activityService;

    private final RedeemCodeImportService redeemCodeImportService;

    public ActivityAdminController(ActivityService activityService, RedeemCodeImportService redeemCodeImportService) {
        this.activityService = activityService;
        this.redeemCodeImportService = redeemCodeImportService;
    }

    @PostMapping
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ActivityDetailResponse> create(
            @Valid @RequestBody ActivityCreateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                activityService.create(request, UserContextHolder.get())
        );
    }

    @PutMapping("/{activityId}")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ActivityDetailResponse> update(
            @PathVariable Long activityId,
            @Valid @RequestBody ActivityUpdateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                activityService.update(activityId, request, UserContextHolder.get())
        );
    }

    @GetMapping("/{activityId}")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ActivityDetailResponse> detail(
            @PathVariable Long activityId,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(requestId(httpServletRequest), activityService.getDetail(activityId));
    }

    @GetMapping
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<List<ActivitySummaryResponse>> list(HttpServletRequest httpServletRequest) {
        return ApiResponse.success(requestId(httpServletRequest), activityService.list());
    }

    @PostMapping("/{activityId}/publish")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ActivityDetailResponse> publish(
            @PathVariable Long activityId,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                activityService.publish(activityId, UserContextHolder.get())
        );
    }

    @PostMapping("/{activityId}/advance-publish")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ActivityDetailResponse> advancePublish(
            @PathVariable Long activityId,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                activityService.advancePublish(activityId, UserContextHolder.get())
        );
    }

    @PostMapping("/{activityId}/offline")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ActivityDetailResponse> offline(
            @PathVariable Long activityId,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                activityService.offline(activityId, UserContextHolder.get())
        );
    }

    @DeleteMapping("/{activityId}")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<Void> delete(
            @PathVariable Long activityId,
            HttpServletRequest httpServletRequest
    ) {
        activityService.delete(activityId, UserContextHolder.get());
        return ApiResponse.success(requestId(httpServletRequest), null);
    }

    @PostMapping(path = "/{activityId}/codes/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<RedeemCodeImportBatchDetailResponse> importCodes(
            @PathVariable Long activityId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                redeemCodeImportService.importCodes(activityId, file, UserContextHolder.get())
        );
    }

    @GetMapping("/{activityId}/codes/import-batches")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<List<RedeemCodeImportBatchSummaryResponse>> listImportBatches(
            @PathVariable Long activityId,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                redeemCodeImportService.listBatches(activityId)
        );
    }

    @GetMapping("/{activityId}/codes/import-batches/{batchNo}")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<RedeemCodeImportBatchDetailResponse> importBatchDetail(
            @PathVariable Long activityId,
            @PathVariable String batchNo,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                redeemCodeImportService.getBatchDetail(activityId, batchNo)
        );
    }

    private String requestId(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getHeader(REQUEST_ID_HEADER);
    }
}
