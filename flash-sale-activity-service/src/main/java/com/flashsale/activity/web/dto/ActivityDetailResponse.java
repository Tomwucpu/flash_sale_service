package com.flashsale.activity.web.dto;

import com.flashsale.activity.domain.ActivityEntity;
import com.flashsale.activity.domain.ActivityPhase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActivityDetailResponse(
        Long id,
        String title,
        String description,
        String coverUrl,
        Integer totalStock,
        Integer availableStock,
        BigDecimal priceAmount,
        Boolean needPayment,
        String purchaseLimitType,
        Integer purchaseLimitCount,
        String codeSourceMode,
        String publishMode,
        String publishStatus,
        String phase,
        LocalDateTime publishTime,
        LocalDateTime startTime,
        LocalDateTime endTime
) {

    public static ActivityDetailResponse fromEntity(ActivityEntity entity, ActivityPhase phase) {
        return new ActivityDetailResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCoverUrl(),
                entity.getTotalStock(),
                entity.getAvailableStock(),
                entity.getPriceAmount(),
                entity.getNeedPayment(),
                entity.getPurchaseLimitType(),
                entity.getPurchaseLimitCount(),
                entity.getCodeSourceMode(),
                entity.getPublishMode(),
                entity.getPublishStatus(),
                phase.name(),
                entity.getPublishTime(),
                entity.getStartTime(),
                entity.getEndTime()
        );
    }
}
