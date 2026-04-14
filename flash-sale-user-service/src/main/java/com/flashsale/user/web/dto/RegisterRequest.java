package com.flashsale.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名长度不能超过64位")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需在8到64位之间")
        String password,
        @Size(max = 64, message = "昵称长度不能超过64位")
        String nickname,
        @Size(max = 32, message = "手机号长度不能超过32位")
        String phone
) {
}
