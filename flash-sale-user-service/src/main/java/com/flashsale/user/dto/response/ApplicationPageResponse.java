package com.flashsale.user.dto.response;

import java.util.List;

public record ApplicationPageResponse(
        List<PublisherApplicationResponse> records,
        long total,
        int page,
        int size
) {
}
