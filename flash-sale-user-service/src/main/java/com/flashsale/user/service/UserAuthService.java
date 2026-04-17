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

/**
 * 用户认证服务，负责注册与登录相关核心流程。
 */
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

    /**
     * 用户注册：
     * 1. 校验用户名唯一性
     * 2. 持久化用户基础信息（含密码加密）
     * 3. 返回用户资料
     */
    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        // 用户名唯一性校验
        long existingCount = userMapper.selectCount(
                new LambdaQueryWrapper<UserEntity>()
                        // 条件：数据库中的 username = 前端传来的 username
                        .eq(UserEntity::getUsername, request.username())
        );
        if (existingCount > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(request.username());
        // 落库前对明文密码进行加密
        userEntity.setPasswordHash(passwordEncoder.encode(request.password()));
        // 新注册用户默认普通用户角色且状态为启用
        userEntity.setRole(UserRole.USER.name());
        userEntity.setStatus(UserStatus.ENABLED.name());
        userEntity.setNickname(request.nickname());
        userEntity.setPhone(request.phone());
        userMapper.insert(userEntity);
        return UserProfileResponse.fromEntity(userEntity);
    }

    /**
     * 用户登录：
     * 1. 校验账号密码
     * 2. 校验账号状态
     * 3. 签发访问令牌并返回用户信息
     */
    public LoginResponse login(LoginRequest request) {
        // 按用户名查询有效用户
        UserEntity userEntity = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, request.username())
                        // 逻辑删除标记必须为 0，说明该用户未被删除
                        .eq(UserEntity::getIsDeleted, 0)
        );
        // 统一提示，避免暴露“用户名不存在”或“密码错误”的具体原因
        if (userEntity == null || !passwordEncoder.matches(request.password(), userEntity.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        // 禁用状态账号不允许登录
        if (!UserStatus.ENABLED.name().equals(userEntity.getStatus())) {
            throw new IllegalArgumentException("当前用户已被禁用");
        }

        // 登录成功后签发 JWT 访问令牌
        String accessToken = jwtTokenService.generateToken(
                new UserContext(userEntity.getId(), userEntity.getUsername(), userEntity.getRole())
        );
        return new LoginResponse(accessToken, UserProfileResponse.fromEntity(userEntity));
    }
}
