package com.flashsale.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.activity.domain.ActivityEntity;
import com.flashsale.activity.domain.ActivityPhase;
import com.flashsale.activity.domain.CodeSourceMode;
import com.flashsale.activity.domain.PublishMode;
import com.flashsale.activity.domain.PublishStatus;
import com.flashsale.activity.domain.PurchaseLimitType;
import com.flashsale.activity.mapper.ActivityMapper;
import com.flashsale.activity.mapper.RedeemCodeMapper;
import com.flashsale.activity.web.dto.ActivityCreateRequest;
import com.flashsale.activity.web.dto.ActivityDetailResponse;
import com.flashsale.activity.web.dto.ActivitySummaryResponse;
import com.flashsale.activity.web.dto.ActivityUpdateRequest;
import com.flashsale.common.security.context.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityService {

    private final ActivityMapper activityMapper;

    private final RedeemCodeMapper redeemCodeMapper;

    private final ActivityCacheService activityCacheService;

    public ActivityService(
            ActivityMapper activityMapper,
            RedeemCodeMapper redeemCodeMapper,
            ActivityCacheService activityCacheService
    ) {
        this.activityMapper = activityMapper;
        this.redeemCodeMapper = redeemCodeMapper;
        this.activityCacheService = activityCacheService;
    }

    @Transactional
    public ActivityDetailResponse create(ActivityCreateRequest request, UserContext userContext) {
        validateRequest(
                request.totalStock(),
                request.priceAmount(),
                request.needPayment(),
                request.purchaseLimitType(),
                request.codeSourceMode(),
                request.publishMode(),
                request.publishTime(),
                request.startTime(),
                request.endTime()
        );

        ActivityEntity activity = new ActivityEntity();
        fillFromCreateRequest(activity, request);
        activity.setAvailableStock(request.totalStock());
        activity.setPublishStatus(PublishStatus.UNPUBLISHED.name());
        activity.setVersion(0);
        activity.setIsDeleted(0);
        Long operatorId = operatorId(userContext);
        activity.setCreatedBy(operatorId);
        activity.setUpdatedBy(operatorId);
        activityMapper.insert(activity);
        return toDetailResponse(activity);
    }

    @Transactional
    public ActivityDetailResponse update(Long activityId, ActivityUpdateRequest request, UserContext userContext) {
        ActivityEntity activity = getRequiredActivity(activityId);
        if (!PublishStatus.UNPUBLISHED.name().equals(activity.getPublishStatus())) {
            throw new IllegalArgumentException("仅未发布活动允许编辑");
        }

        validateRequest(
                request.totalStock(),
                request.priceAmount(),
                request.needPayment(),
                request.purchaseLimitType(),
                request.codeSourceMode(),
                request.publishMode(),
                request.publishTime(),
                request.startTime(),
                request.endTime()
        );

        fillFromUpdateRequest(activity, request);
        activity.setAvailableStock(request.totalStock());
        activity.setUpdatedBy(operatorId(userContext));
        activityMapper.updateById(activity);
        return toDetailResponse(activity);
    }

    public ActivityDetailResponse getDetail(Long activityId) {
        return toDetailResponse(getRequiredActivity(activityId));
    }

    public List<ActivitySummaryResponse> list() {
        return activityMapper.selectList(
                        new LambdaQueryWrapper<ActivityEntity>()
                                .eq(ActivityEntity::getIsDeleted, 0)
                                .orderByDesc(ActivityEntity::getId)
                ).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public List<ActivitySummaryResponse> listPublicActivities() {
        return activityMapper.selectList(
                        new LambdaQueryWrapper<ActivityEntity>()
                                .eq(ActivityEntity::getIsDeleted, 0)
                                .eq(ActivityEntity::getPublishStatus, PublishStatus.PUBLISHED.name())
                                .orderByDesc(ActivityEntity::getStartTime)
                                .orderByDesc(ActivityEntity::getId)
                ).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public ActivityDetailResponse getPublicDetail(Long activityId) {
        return toDetailResponse(getRequiredPublishedActivity(activityId));
    }

    @Transactional
    public ActivityDetailResponse publish(Long activityId, UserContext userContext) {
        ActivityEntity activity = getRequiredActivity(activityId);
        validatePublish(activity);

        if (PublishMode.SCHEDULED.name().equals(activity.getPublishMode()) && activity.getPublishTime().isAfter(LocalDateTime.now())) {
            activity.setUpdatedBy(operatorId(userContext));
            activityMapper.updateById(activity);
            return toDetailResponse(activity);
        }

        return toDetailResponse(doPublish(activity, operatorId(userContext), true));
    }

    @Transactional
    public ActivityDetailResponse offline(Long activityId, UserContext userContext) {
        ActivityEntity activity = getRequiredActivity(activityId);
        activity.setPublishStatus(PublishStatus.OFFLINE.name());
        activity.setUpdatedBy(operatorId(userContext));
        activityMapper.updateById(activity);
        activityCacheService.clear(activity);
        return toDetailResponse(activity);
    }

    @Transactional
    public void delete(Long activityId, UserContext userContext) {
        ActivityEntity activity = getRequiredActivity(activityId);
        if (!PublishStatus.UNPUBLISHED.name().equals(activity.getPublishStatus())
                && !PublishStatus.OFFLINE.name().equals(activity.getPublishStatus())) {
            throw new IllegalArgumentException("仅未发布或已下线活动允许删除");
        }

        activity.setIsDeleted(1);
        activity.setUpdatedBy(operatorId(userContext));
        activityMapper.updateById(activity);
        activityCacheService.clear(activity);
    }

    @Transactional
    public void publishReadyActivities() {
        List<ActivityEntity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<ActivityEntity>()
                        .eq(ActivityEntity::getIsDeleted, 0)
                        .eq(ActivityEntity::getPublishMode, PublishMode.SCHEDULED.name())
                        .eq(ActivityEntity::getPublishStatus, PublishStatus.UNPUBLISHED.name())
                        .le(ActivityEntity::getPublishTime, LocalDateTime.now())
                        .orderByAsc(ActivityEntity::getPublishTime)
        );

        for (ActivityEntity activity : activities) {
            validatePublish(activity);
            doPublish(activity, activity.getUpdatedBy(), false);
        }
    }

    private ActivityEntity doPublish(ActivityEntity activity, Long operatorId, boolean immediatePublishRequest) {
        if (PublishStatus.PUBLISHED.name().equals(activity.getPublishStatus())) {
            return activity;
        }

        if (immediatePublishRequest && PublishMode.IMMEDIATE.name().equals(activity.getPublishMode())) {
            activity.setPublishTime(LocalDateTime.now());
        }

        activity.setPublishStatus(PublishStatus.PUBLISHED.name());
        activity.setUpdatedBy(operatorId);
        activityMapper.updateById(activity);
        activityCacheService.warmUp(activity);
        return activity;
    }

    private void validatePublish(ActivityEntity activity) {
        if (PublishStatus.OFFLINE.name().equals(activity.getPublishStatus())) {
            throw new IllegalArgumentException("已下线活动不能发布");
        }

        validateRequest(
                activity.getTotalStock(),
                activity.getPriceAmount(),
                activity.getNeedPayment(),
                activity.getPurchaseLimitType(),
                activity.getCodeSourceMode(),
                activity.getPublishMode(),
                activity.getPublishTime(),
                activity.getStartTime(),
                activity.getEndTime()
        );

        if (CodeSourceMode.THIRD_PARTY_IMPORTED.name().equals(activity.getCodeSourceMode())) {
            long availableCodeCount = redeemCodeMapper.countAvailableCodes(activity.getId());
            if (availableCodeCount < activity.getTotalStock()) {
                throw new IllegalArgumentException("第三方兑换码可用数量不足");
            }
        }
    }

    private void validateRequest(
            Integer totalStock,
            BigDecimal priceAmount,
            Boolean needPayment,
            String purchaseLimitType,
            String codeSourceMode,
            String publishMode,
            LocalDateTime publishTime,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (publishTime.isAfter(startTime)) {
            throw new IllegalArgumentException("发布时间不能晚于活动开始时间");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("活动开始时间必须早于结束时间");
        }
        if (!priceRuleValid(priceAmount, needPayment)) {
            throw new IllegalArgumentException("活动金额与支付模式不匹配");
        }

        PurchaseLimitType.valueOf(purchaseLimitType);
        CodeSourceMode.valueOf(codeSourceMode);
        PublishMode.valueOf(publishMode);
        if (totalStock <= 0) {
            throw new IllegalArgumentException("活动库存必须大于0");
        }
    }

    private boolean priceRuleValid(BigDecimal priceAmount, Boolean needPayment) {
        if (Boolean.TRUE.equals(needPayment)) {
            return priceAmount.compareTo(BigDecimal.ZERO) > 0;
        }
        return priceAmount.compareTo(BigDecimal.ZERO) == 0;
    }

    private ActivityEntity getRequiredActivity(Long activityId) {
        ActivityEntity activity = activityMapper.selectOne(
                new LambdaQueryWrapper<ActivityEntity>()
                        .eq(ActivityEntity::getId, activityId)
                        .eq(ActivityEntity::getIsDeleted, 0)
        );
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        return activity;
    }

    private ActivityEntity getRequiredPublishedActivity(Long activityId) {
        ActivityEntity activity = activityMapper.selectOne(
                new LambdaQueryWrapper<ActivityEntity>()
                        .eq(ActivityEntity::getId, activityId)
                        .eq(ActivityEntity::getIsDeleted, 0)
                        .eq(ActivityEntity::getPublishStatus, PublishStatus.PUBLISHED.name())
        );
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        return activity;
    }

    private ActivitySummaryResponse toSummaryResponse(ActivityEntity activity) {
        return ActivitySummaryResponse.fromEntity(activity, phaseOf(activity));
    }

    private ActivityDetailResponse toDetailResponse(ActivityEntity activity) {
        return ActivityDetailResponse.fromEntity(activity, phaseOf(activity));
    }

    private ActivityPhase phaseOf(ActivityEntity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            return ActivityPhase.PREVIEW;
        }
        if (now.isAfter(activity.getEndTime())) {
            return ActivityPhase.ENDED;
        }
        return ActivityPhase.ONGOING;
    }

    private void fillFromCreateRequest(ActivityEntity activity, ActivityCreateRequest request) {
        activity.setTitle(request.title());
        activity.setDescription(request.description());
        activity.setCoverUrl(request.coverUrl());
        activity.setTotalStock(request.totalStock());
        activity.setPriceAmount(request.priceAmount());
        activity.setNeedPayment(request.needPayment());
        activity.setPurchaseLimitType(request.purchaseLimitType());
        activity.setPurchaseLimitCount(request.purchaseLimitCount());
        activity.setCodeSourceMode(request.codeSourceMode());
        activity.setPublishMode(request.publishMode());
        activity.setPublishTime(request.publishTime());
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
    }

    private void fillFromUpdateRequest(ActivityEntity activity, ActivityUpdateRequest request) {
        activity.setTitle(request.title());
        activity.setDescription(request.description());
        activity.setCoverUrl(request.coverUrl());
        activity.setTotalStock(request.totalStock());
        activity.setPriceAmount(request.priceAmount());
        activity.setNeedPayment(request.needPayment());
        activity.setPurchaseLimitType(request.purchaseLimitType());
        activity.setPurchaseLimitCount(request.purchaseLimitCount());
        activity.setCodeSourceMode(request.codeSourceMode());
        activity.setPublishMode(request.publishMode());
        activity.setPublishTime(request.publishTime());
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
    }

    private Long operatorId(UserContext userContext) {
        return userContext == null ? null : userContext.userId();
    }
}
