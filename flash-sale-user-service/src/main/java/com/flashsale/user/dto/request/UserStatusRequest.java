package com.flashsale.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserStatusRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
