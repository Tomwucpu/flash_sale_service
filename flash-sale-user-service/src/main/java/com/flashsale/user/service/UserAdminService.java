package com.flashsale.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.user.domain.ApplicationStatus;
import com.flashsale.user.domain.PublisherApplicationEntity;
import com.flashsale.user.domain.UserEntity;
import com.flashsale.user.domain.UserRole;
import com.flashsale.user.domain.UserStatus;
import com.flashsale.user.mapper.PublisherApplicationMapper;
import com.flashsale.user.mapper.UserMapper;
import com.flashsale.user.dto.request.ApplicationPageRequest;
import com.flashsale.user.dto.request.UserPageRequest;
import com.flashsale.user.dto.response.ApplicationPageResponse;
import com.flashsale.user.dto.response.PublisherApplicationResponse;
import com.flashsale.user.dto.response.UserPageResponse;
import com.flashsale.user.dto.response.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
public class UserAdminService {

    private final UserMapper userMapper;
    private final PublisherApplicationMapper applicationMapper;

    public UserAdminService(UserMapper userMapper, PublisherApplicationMapper applicationMapper) {
        this.userMapper = userMapper;
        this.applicationMapper = applicationMapper;
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

    public ApplicationPageResponse listApplications(ApplicationPageRequest request) {
        LambdaQueryWrapper<PublisherApplicationEntity> queryWrapper =
                new LambdaQueryWrapper<PublisherApplicationEntity>()
                        .eq(PublisherApplicationEntity::getIsDeleted, 0);

        if (StringUtils.hasText(request.status())) {
            queryWrapper.eq(PublisherApplicationEntity::getStatus, request.status());
        }

        queryWrapper.orderByDesc(PublisherApplicationEntity::getId);

        Page<PublisherApplicationEntity> page = new Page<>(request.getPage(), request.getSize());
        IPage<PublisherApplicationEntity> result = applicationMapper.selectPage(page, queryWrapper);

        return new ApplicationPageResponse(
                result.getRecords().stream().map(entity -> {
                    UserEntity user = userMapper.selectById(entity.getUserId());
                    String reviewerName = null;
                    if (entity.getReviewerId() != null) {
                        UserEntity reviewer = userMapper.selectById(entity.getReviewerId());
                        reviewerName = reviewer != null ? reviewer.getUsername() : null;
                    }
                    return PublisherApplicationResponse.fromEntity(
                            entity,
                            user != null ? user.getUsername() : null,
                            reviewerName
                    );
                }).toList(),
                result.getTotal(),
                request.getPage(),
                request.getSize()
        );
    }

    @Transactional
    public PublisherApplicationResponse approveApplication(Long applicationId, String reviewNote, UserContext operator) {
        PublisherApplicationEntity entity = applicationMapper.selectOne(
                new LambdaQueryWrapper<PublisherApplicationEntity>()
                        .eq(PublisherApplicationEntity::getId, applicationId)
                        .eq(PublisherApplicationEntity::getStatus, ApplicationStatus.PENDING.name())
                        .eq(PublisherApplicationEntity::getIsDeleted, 0)
        );
        if (entity == null) {
            throw new IllegalArgumentException("申请不存在或已被处理");
        }

        entity.setStatus(ApplicationStatus.APPROVED.name());
        entity.setReviewNote(reviewNote);
        entity.setReviewerId(operator.userId());
        entity.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(entity);

        UserEntity user = userMapper.selectById(entity.getUserId());
        if (user != null) {
            user.setRole(UserRole.PUBLISHER.name());
            userMapper.updateById(user);
        }

        return PublisherApplicationResponse.fromEntity(
                entity,
                user != null ? user.getUsername() : null,
                operator.username()
        );
    }

    @Transactional
    public PublisherApplicationResponse rejectApplication(Long applicationId, String reviewNote, UserContext operator) {
        PublisherApplicationEntity entity = applicationMapper.selectOne(
                new LambdaQueryWrapper<PublisherApplicationEntity>()
                        .eq(PublisherApplicationEntity::getId, applicationId)
                        .eq(PublisherApplicationEntity::getStatus, ApplicationStatus.PENDING.name())
                        .eq(PublisherApplicationEntity::getIsDeleted, 0)
        );
        if (entity == null) {
            throw new IllegalArgumentException("申请不存在或已被处理");
        }

        entity.setStatus(ApplicationStatus.REJECTED.name());
        entity.setReviewNote(reviewNote);
        entity.setReviewerId(operator.userId());
        entity.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(entity);

        UserEntity user = userMapper.selectById(entity.getUserId());

        return PublisherApplicationResponse.fromEntity(
                entity,
                user != null ? user.getUsername() : null,
                operator.username()
        );
    }
}
