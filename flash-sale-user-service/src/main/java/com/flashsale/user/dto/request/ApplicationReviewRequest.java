package com.flashsale.user.dto.request;

import jakarta.validation.constraints.Size;

public record ApplicationReviewRequest(
        @Size(max = 500, message = "审核意见最多 500 字")
        String reviewNote
) {
}
