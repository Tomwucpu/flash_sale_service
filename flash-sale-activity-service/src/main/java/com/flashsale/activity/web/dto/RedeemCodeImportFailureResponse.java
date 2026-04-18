package com.flashsale.activity.web.dto;

import com.flashsale.activity.domain.RedeemCodeImportFailureEntity;

public record RedeemCodeImportFailureResponse(
        Integer lineNumber,
        String rawCode,
        String reason
) {
    public static RedeemCodeImportFailureResponse fromEntity(RedeemCodeImportFailureEntity entity) {
        return new RedeemCodeImportFailureResponse(
                entity.getLineNo(),
                entity.getRawCode(),
                entity.getFailureReason()
        );
    }
}
