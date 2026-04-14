package com.flashsale.user.web.dto;

import com.flashsale.user.domain.UserEntity;

public record UserProfileResponse(
        Long id,
        String username,
        String role,
        String status,
        String nickname,
        String phone
) {

    public static UserProfileResponse fromEntity(UserEntity userEntity) {
        return new UserProfileResponse(
                userEntity.getId(),
                userEntity.getUsername(),
                userEntity.getRole(),
                userEntity.getStatus(),
                userEntity.getNickname(),
                userEntity.getPhone()
        );
    }
}
