package com.flashsale.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublisherApplicationRequest(
        @NotBlank(message = "申请理由不能为空")
        @Size(max = 500, message = "申请理由最多 500 字")
        String reason
) {
}
