package com.flashsale.user.dto.response;

import com.flashsale.user.domain.PublisherApplicationEntity;
import java.time.LocalDateTime;

public record PublisherApplicationResponse(
        Long id,
        Long userId,
        String username,
        String reason,
        String status,
        String reviewNote,
        Long reviewerId,
        String reviewerName,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {

    public static PublisherApplicationResponse fromEntity(
            PublisherApplicationEntity entity,
            String username,
            String reviewerName
    ) {
        return new PublisherApplicationResponse(
                entity.getId(),
                entity.getUserId(),
                username,
                entity.getReason(),
                entity.getStatus(),
                entity.getReviewNote(),
                entity.getReviewerId(),
                reviewerName,
                entity.getReviewedAt(),
                entity.getCreatedAt()
        );
    }
}
