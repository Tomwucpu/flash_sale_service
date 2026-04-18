package com.flashsale.activity.web.dto;

import com.flashsale.activity.domain.RedeemCodeImportBatchEntity;

public record RedeemCodeImportBatchSummaryResponse(
        String batchNo,
        String fileName,
        Integer totalCount,
        Integer successCount,
        Integer failedCount
) {
    public static RedeemCodeImportBatchSummaryResponse fromEntity(RedeemCodeImportBatchEntity entity) {
        return new RedeemCodeImportBatchSummaryResponse(
                entity.getBatchNo(),
                entity.getFileName(),
                entity.getTotalCount(),
                entity.getSuccessCount(),
                entity.getFailedCount()
        );
    }
}
