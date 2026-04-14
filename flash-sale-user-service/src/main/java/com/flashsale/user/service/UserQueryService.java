package com.flashsale.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.exception.UnauthorizedException;
import com.flashsale.user.domain.UserEntity;
import com.flashsale.user.mapper.UserMapper;
import com.flashsale.user.web.dto.UserProfileResponse;
import org.springframework.stereotype.Service;

@Service
public class UserQueryService {

    private final UserMapper userMapper;

    public UserQueryService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserProfileResponse currentUser(UserContext userContext) {
        if (userContext == null || userContext.userId() == null || userContext.userId() <= 0) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }
        return getUserById(userContext.userId());
    }

    public UserProfileResponse getUserById(Long userId) {
        UserEntity userEntity = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getId, userId)
                        .eq(UserEntity::getIsDeleted, 0)
        );
        if (userEntity == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return UserProfileResponse.fromEntity(userEntity);
    }
}
