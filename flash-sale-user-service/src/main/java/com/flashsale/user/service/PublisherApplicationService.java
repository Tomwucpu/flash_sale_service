package com.flashsale.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.user.domain.ApplicationStatus;
import com.flashsale.user.domain.PublisherApplicationEntity;
import com.flashsale.user.domain.UserEntity;
import com.flashsale.user.domain.UserRole;
import com.flashsale.user.mapper.PublisherApplicationMapper;
import com.flashsale.user.mapper.UserMapper;
import com.flashsale.user.dto.request.ApplicationPageRequest;
import com.flashsale.user.dto.request.PublisherApplicationRequest;
import com.flashsale.user.dto.response.ApplicationPageResponse;
import com.flashsale.user.dto.response.PublisherApplicationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
public class PublisherApplicationService {

    private final PublisherApplicationMapper applicationMapper;
    private final UserMapper userMapper;

    public PublisherApplicationService(PublisherApplicationMapper applicationMapper, UserMapper userMapper) {
        this.applicationMapper = applicationMapper;
        this.userMapper = userMapper;
    }

    /**
     * 用户提交发布者申请
     */
    @Transactional
    public PublisherApplicationResponse apply(UserContext userContext, PublisherApplicationRequest request) {
        UserEntity user = currentUserEntity(userContext);

        // 已经是发布者或管理员，无需申请
        if (UserRole.PUBLISHER.name().equals(user.getRole())
                || UserRole.ADMIN.name().equals(user.getRole())) {
            throw new IllegalArgumentException("您已经是发布者或管理员，无需申请");
        }

        // 检查是否有待审批的申请
        long pendingCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<PublisherApplicationEntity>()
                        .eq(PublisherApplicationEntity::getUserId, userContext.userId())
                        .eq(PublisherApplicationEntity::getStatus, ApplicationStatus.PENDING.name())
                        .eq(PublisherApplicationEntity::getIsDeleted, 0)
        );
        if (pendingCount > 0) {
            throw new IllegalArgumentException("您已有待审批的申请，请耐心等待");
        }

        PublisherApplicationEntity entity = new PublisherApplicationEntity();
        entity.setUserId(userContext.userId());
        entity.setReason(request.reason());
        entity.setStatus(ApplicationStatus.PENDING.name());
        applicationMapper.insert(entity);

        return PublisherApplicationResponse.fromEntity(entity, user.getUsername(), null);
    }

    /**
     * 用户查看自己的申请状态
     */
    public PublisherApplicationResponse getMyApplication(UserContext userContext) {
        PublisherApplicationEntity entity = applicationMapper.selectOne(
                new LambdaQueryWrapper<PublisherApplicationEntity>()
                        .eq(PublisherApplicationEntity::getUserId, userContext.userId())
                        .eq(PublisherApplicationEntity::getIsDeleted, 0)
                        .orderByDesc(PublisherApplicationEntity::getId)
                        .last("LIMIT 1")
        );
        if (entity == null) {
            throw new IllegalArgumentException("暂无申请记录");
        }

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
    }

    /**
     * 管理员查看申请列表
     */
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

    /**
     * 管理员批准申请
     */
    @Transactional
    public PublisherApplicationResponse approve(Long applicationId, String reviewNote, UserContext operator) {
        PublisherApplicationEntity entity = getPendingApplication(applicationId);

        // 更新申请状态
        entity.setStatus(ApplicationStatus.APPROVED.name());
        entity.setReviewNote(reviewNote);
        entity.setReviewerId(operator.userId());
        entity.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(entity);

        // 将用户角色升级为发布者
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

    /**
     * 管理员拒绝申请
     */
    @Transactional
    public PublisherApplicationResponse reject(Long applicationId, String reviewNote, UserContext operator) {
        PublisherApplicationEntity entity = getPendingApplication(applicationId);

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

    private PublisherApplicationEntity getPendingApplication(Long applicationId) {
        PublisherApplicationEntity entity = applicationMapper.selectOne(
                new LambdaQueryWrapper<PublisherApplicationEntity>()
                        .eq(PublisherApplicationEntity::getId, applicationId)
                        .eq(PublisherApplicationEntity::getStatus, ApplicationStatus.PENDING.name())
                        .eq(PublisherApplicationEntity::getIsDeleted, 0)
        );
        if (entity == null) {
            throw new IllegalArgumentException("申请不存在或已被处理");
        }
        return entity;
    }

    private UserEntity currentUserEntity(UserContext userContext) {
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getId, userContext.userId())
                        .eq(UserEntity::getIsDeleted, 0)
        );
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }
}
