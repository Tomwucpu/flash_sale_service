package com.flashsale.activity.dto.response;

import java.util.List;

public record ActivityPageResponse(
        List<ActivitySummaryResponse> records,
        long total,
        int page,
        int size,
        long totalCount,
        long unpublishedCount,
        long publishedCount
) {
}
