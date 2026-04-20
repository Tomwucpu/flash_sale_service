package com.flashsale.order.web;

import jakarta.validation.constraints.NotBlank;

public record CompensationResolveRequest(
        @NotBlank(message = "resolutionNote 不能为空") String resolutionNote
) {
}
