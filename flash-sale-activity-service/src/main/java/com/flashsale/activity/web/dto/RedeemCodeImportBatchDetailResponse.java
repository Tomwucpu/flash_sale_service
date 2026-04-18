package com.flashsale.activity.web.dto;

import com.flashsale.activity.domain.RedeemCodeImportBatchEntity;

import java.util.List;

public record RedeemCodeImportBatchDetailResponse(
        String batchNo,
        String fileName,
        Integer totalCount,
        Integer successCount,
        Integer failedCount,
        List<RedeemCodeImportFailureResponse> failures
) {
    public static RedeemCodeImportBatchDetailResponse fromEntity(
            RedeemCodeImportBatchEntity entity,
            List<RedeemCodeImportFailureResponse> failures
    ) {
        return new RedeemCodeImportBatchDetailResponse(
                entity.getBatchNo(),
                entity.getFileName(),
                entity.getTotalCount(),
                entity.getSuccessCount(),
                entity.getFailedCount(),
                failures
        );
    }
}
