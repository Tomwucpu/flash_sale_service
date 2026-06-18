package com.flashsale.user.dto.response;

import java.util.List;

public record UserPageResponse(
        List<UserProfileResponse> records,
        long total,
        int page,
        int size
) {
}
