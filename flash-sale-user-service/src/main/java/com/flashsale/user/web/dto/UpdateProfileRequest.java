package com.flashsale.user.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 64, message = "昵称长度不能超过64位")
        String nickname,
        @Size(max = 32, message = "手机号长度不能超过32位")
        String phone
) {
}
