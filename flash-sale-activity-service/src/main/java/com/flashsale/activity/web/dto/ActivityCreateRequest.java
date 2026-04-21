package com.flashsale.activity.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActivityCreateRequest(
        @NotBlank(message = "活动标题不能为空")
        @Size(max = 128, message = "活动标题长度不能超过128位")
        String title,
        @Size(max = 2000, message = "活动描述长度不能超过2000位")
        String description,
        @Size(max = 255, message = "封面图地址长度不能超过255位")
        String coverUrl,
        @NotNull(message = "活动库存不能为空")
        @Positive(message = "活动库存必须大于0")
        Integer totalStock,
        @NotNull(message = "活动金额不能为空")
        @PositiveOrZero(message = "活动金额不能小于0")
        BigDecimal priceAmount,
        @NotNull(message = "支付模式不能为空")
        Boolean needPayment,
        @NotBlank(message = "限购类型不能为空")
        String purchaseLimitType,
        @NotNull(message = "限购次数不能为空")
        @Positive(message = "限购次数必须大于0")
        Integer purchaseLimitCount,
        @NotBlank(message = "兑换码来源不能为空")
        String codeSourceMode,
        @NotBlank(message = "发布模式不能为空")
        String publishMode,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime publishTime,
        @NotNull(message = "活动开始时间不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startTime,
        @NotNull(message = "活动结束时间不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime endTime
) {
}
