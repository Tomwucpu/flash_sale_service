package com.flashsale.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.jwt.JwtTokenService;
import com.flashsale.user.domain.UserEntity;
import com.flashsale.user.domain.UserRole;
import com.flashsale.user.domain.UserStatus;
import com.flashsale.user.mapper.UserMapper;
import com.flashsale.user.web.dto.LoginRequest;
import com.flashsale.user.web.dto.LoginResponse;
import com.flashsale.user.web.dto.RegisterRequest;
import com.flashsale.user.web.dto.UserProfileResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuthService {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenService jwtTokenService;

    public UserAuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        long existingCount = userMapper.selectCount(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, request.username())
        );
        if (existingCount > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(request.username());
        userEntity.setPasswordHash(passwordEncoder.encode(request.password()));
        userEntity.setRole(UserRole.USER.name());
        userEntity.setStatus(UserStatus.ENABLED.name());
        userEntity.setNickname(request.nickname());
        userEntity.setPhone(request.phone());
        userMapper.insert(userEntity);
        return UserProfileResponse.fromEntity(userEntity);
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity userEntity = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, request.username())
                        .eq(UserEntity::getIsDeleted, 0)
        );
        if (userEntity == null || !passwordEncoder.matches(request.password(), userEntity.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!UserStatus.ENABLED.name().equals(userEntity.getStatus())) {
            throw new IllegalArgumentException("当前用户已被禁用");
        }

        String accessToken = jwtTokenService.generateToken(
                new UserContext(userEntity.getId(), userEntity.getUsername(), userEntity.getRole())
        );
        return new LoginResponse(accessToken, UserProfileResponse.fromEntity(userEntity));
    }
}
