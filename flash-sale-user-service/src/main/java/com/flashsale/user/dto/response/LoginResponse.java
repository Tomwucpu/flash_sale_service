package com.flashsale.user.dto.response;

public record LoginResponse(String accessToken, UserProfileResponse user) {
}
