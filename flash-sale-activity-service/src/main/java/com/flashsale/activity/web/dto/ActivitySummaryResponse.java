package com.flashsale.activity.web.dto;

import com.flashsale.activity.domain.ActivityEntity;
import com.flashsale.activity.domain.ActivityPhase;

import java.time.LocalDateTime;

public record ActivitySummaryResponse(
        Long id,
        String title,
        Integer totalStock,
        Integer availableStock,
        String publishMode,
        String publishStatus,
        String phase,
        LocalDateTime publishTime,
        LocalDateTime startTime,
        LocalDateTime endTime
) {

    public static ActivitySummaryResponse fromEntity(ActivityEntity entity, ActivityPhase phase) {
        return new ActivitySummaryResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getTotalStock(),
                entity.getAvailableStock(),
                entity.getPublishMode(),
                entity.getPublishStatus(),
                phase.name(),
                entity.getPublishTime(),
                entity.getStartTime(),
                entity.getEndTime()
        );
    }
}
