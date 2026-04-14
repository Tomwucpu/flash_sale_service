package com.flashsale.user.web.dto;

public record LoginResponse(String accessToken, UserProfileResponse user) {
}
