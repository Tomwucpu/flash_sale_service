package com.flashsale.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.user.domain.UserEntity;
import com.flashsale.user.domain.UserRole;
import com.flashsale.user.domain.UserStatus;
import com.flashsale.user.mapper.UserMapper;
import com.flashsale.user.dto.request.UserPageRequest;
import com.flashsale.user.dto.response.UserPageResponse;
import com.flashsale.user.dto.response.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Service
public class UserAdminService {

    private final UserMapper userMapper;

    public UserAdminService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserPageResponse listUsers(UserPageRequest request) {
        LambdaQueryWrapper<UserEntity> queryWrapper = new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getIsDeleted, 0);

        if (StringUtils.hasText(request.keyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(UserEntity::getUsername, request.keyword())
                    .or()
                    .like(UserEntity::getNickname, request.keyword())
                    .or()
                    .like(UserEntity::getPhone, request.keyword())
            );
        }

        if (StringUtils.hasText(request.role())) {
            queryWrapper.eq(UserEntity::getRole, request.role());
        }

        if (StringUtils.hasText(request.status())) {
            queryWrapper.eq(UserEntity::getStatus, request.status());
        }

        queryWrapper.orderByDesc(UserEntity::getId);

        Page<UserEntity> page = new Page<>(request.getPage(), request.getSize());
        IPage<UserEntity> result = userMapper.selectPage(page, queryWrapper);

        return new UserPageResponse(
                result.getRecords().stream().map(UserProfileResponse::fromEntity).toList(),
                result.getTotal(),
                request.getPage(),
                request.getSize()
        );
    }

    @Transactional
    public UserProfileResponse updateStatus(Long userId, String status, UserContext operator) {
        if (operator != null && operator.userId().equals(userId)) {
            throw new IllegalArgumentException("不能操作自己的账号状态");
        }

        if (Arrays.stream(UserStatus.values()).noneMatch(s -> s.name().equals(status))) {
            throw new IllegalArgumentException("无效的状态值：" + status);
        }

        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getId, userId)
                        .eq(UserEntity::getIsDeleted, 0)
        );
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        user.setStatus(status);
        userMapper.updateById(user);

        return UserProfileResponse.fromEntity(user);
    }

    @Transactional
    public UserProfileResponse updateRole(Long userId, String role, UserContext operator) {
        if (operator != null && operator.userId().equals(userId)) {
            throw new IllegalArgumentException("不能修改自己的角色");
        }

        if (Arrays.stream(UserRole.values()).noneMatch(r -> r.name().equals(role))) {
            throw new IllegalArgumentException("无效的角色值：" + role);
        }

        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getId, userId)
                        .eq(UserEntity::getIsDeleted, 0)
        );
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        user.setRole(role);
        userMapper.updateById(user);

        return UserProfileResponse.fromEntity(user);
    }
}
