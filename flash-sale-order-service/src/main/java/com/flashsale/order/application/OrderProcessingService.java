package com.flashsale.order.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.order.domain.ActivityProductEntity;
import com.flashsale.order.domain.OrderRecordEntity;
import com.flashsale.order.domain.RedeemCodeEntity;
import com.flashsale.order.mapper.ActivityProductMapper;
import com.flashsale.order.mapper.OrderRecordMapper;
import com.flashsale.order.mapper.RedeemCodeMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OrderProcessingService {

    private static final String ORDER_STATUS_INIT = "INIT";
    private static final String ORDER_STATUS_CONFIRMED = "CONFIRMED";
    private static final String ORDER_STATUS_CLOSED = "CLOSED";
    private static final String ORDER_STATUS_FAILED = "FAILED";
    private static final String PAY_STATUS_NO_NEED = "NO_NEED";
    private static final String PAY_STATUS_WAIT_PAY = "WAIT_PAY";
    private static final String PAY_STATUS_PAID = "PAID";
    private static final String PAY_STATUS_CLOSED = "CLOSED";
    private static final String CODE_STATUS_PENDING = "PENDING";
    private static final String CODE_STATUS_ISSUED = "ISSUED";
    private static final String CODE_STATUS_FAILED = "FAILED";
    private static final int SYSTEM_CODE_MAX_ATTEMPTS = 5;
    private static final int IMPORTED_CODE_CLAIM_MAX_ATTEMPTS = 3;

    private final ActivityProductMapper activityProductMapper;
    private final OrderRecordMapper orderRecordMapper;
    private final RedeemCodeMapper redeemCodeMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final OrderNoGenerator orderNoGenerator;
    private final RedeemCodeGenerator redeemCodeGenerator;
    private final Clock clock;

    public OrderProcessingService(
            ActivityProductMapper activityProductMapper,
            OrderRecordMapper orderRecordMapper,
            RedeemCodeMapper redeemCodeMapper,
            StringRedisTemplate stringRedisTemplate,
            OrderNoGenerator orderNoGenerator,
            RedeemCodeGenerator redeemCodeGenerator,
            Clock clock
    ) {
        this.activityProductMapper = activityProductMapper;
        this.orderRecordMapper = orderRecordMapper;
        this.redeemCodeMapper = redeemCodeMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.orderNoGenerator = orderNoGenerator;
        this.redeemCodeGenerator = redeemCodeGenerator;
        this.clock = clock;
    }

    @Transactional
    public void handleFreeOrder(OrderCreatePayload payload) {
        ActivityProductEntity activity = loadActivity(payload.activityId());
        if (activity == null) {
            compensateAndWriteFailure(payload, null, FailureReason.ACTIVITY_NOT_FOUND, null);
            return;
        }

        OrderRecordEntity order = orderRecordMapper.findByPurchaseUniqueKey(payload.purchaseUniqueKey());
        if (order == null) {
            try {
                order = createInitOrder(payload, BigDecimal.ZERO, PAY_STATUS_NO_NEED);
            } catch (DataAccessException exception) {
                order = orderRecordMapper.findByPurchaseUniqueKey(payload.purchaseUniqueKey());
                if (order == null) {
                    compensateAndWriteFailure(payload, null, FailureReason.ORDER_CREATE_FAILED, activity);
                    return;
                }
            }
        }

        if (isSuccess(order)) {
            writeSuccess(payload.activityId(), payload.userId(), order, assignedCodeValue(order), activity);
            return;
        }
        if (isFailed(order)) {
            writeFailure(payload.activityId(), payload.userId(), order, FailureReason.fromStoredValue(order.getFailReason()), activity);
            return;
        }

        try {
            String code = issueCode(payload, order);
            markOrderSuccess(order, payload.userId());
            writeSuccess(payload.activityId(), payload.userId(), orderRecordMapper.selectById(order.getId()), code, activity);
        } catch (BusinessFailureException exception) {
            markOrderFailed(order, exception.reason(), payload.userId(), null);
            compensate(payload);
            writeFailure(payload.activityId(), payload.userId(), orderRecordMapper.selectById(order.getId()), exception.reason(), activity);
        } catch (DataAccessException exception) {
            if (order.getId() != null) {
                markOrderFailed(order, FailureReason.ORDER_CREATE_FAILED, payload.userId(), null);
                compensate(payload);
                writeFailure(payload.activityId(), payload.userId(), orderRecordMapper.selectById(order.getId()), FailureReason.ORDER_CREATE_FAILED, activity);
                return;
            }
            compensateAndWriteFailure(payload, null, FailureReason.ORDER_CREATE_FAILED, activity);
        }
    }

    @Transactional
    public void handlePaymentOrder(OrderCreatePayload payload) {
        ActivityProductEntity activity = loadActivity(payload.activityId());
        if (activity == null) {
            compensateAndWriteFailure(payload, null, FailureReason.ACTIVITY_NOT_FOUND, null);
            return;
        }

        OrderRecordEntity order = orderRecordMapper.findByPurchaseUniqueKey(payload.purchaseUniqueKey());
        if (order == null) {
            try {
                order = createInitOrder(payload, activity.getPriceAmount(), PAY_STATUS_WAIT_PAY);
            } catch (DataAccessException exception) {
                order = orderRecordMapper.findByPurchaseUniqueKey(payload.purchaseUniqueKey());
                if (order == null) {
                    compensateAndWriteFailure(payload, null, FailureReason.ORDER_CREATE_FAILED, activity);
                    return;
                }
            }
        }

        if (isSuccess(order)) {
            writeSuccess(payload.activityId(), payload.userId(), order, assignedCodeValue(order), activity);
            return;
        }
        if (isClosed(order)) {
            writeFailure(payload.activityId(), payload.userId(), order, FailureReason.PAYMENT_TIMEOUT, activity);
            return;
        }
        if (isFailed(order)) {
            writeFailure(payload.activityId(), payload.userId(), order, FailureReason.fromStoredValue(order.getFailReason()), activity);
            return;
        }
        writePendingPayment(payload.activityId(), payload.userId(), order, activity);
    }

    @Transactional
    public void handlePaymentSuccess(PaymentSuccessPayload payload) {
        OrderRecordEntity order = orderRecordMapper.findByOrderNo(payload.orderNo());
        if (order == null) {
            return;
        }
        ActivityProductEntity activity = loadActivity(order.getActivityId());
        if (activity == null) {
            return;
        }
        if (isClosed(order)) {
            return;
        }
        if (isSuccess(order)) {
            writeSuccess(order.getActivityId(), order.getUserId(), order, assignedCodeValue(order), activity);
            return;
        }
        if (!PAY_STATUS_PAID.equals(order.getPayStatus())) {
            markOrderPaid(order, order.getUserId());
        }

        try {
            String code = issueCode(activity, order);
            markOrderSuccess(order, order.getUserId());
            writeSuccess(order.getActivityId(), order.getUserId(), orderRecordMapper.selectById(order.getId()), code, activity);
        } catch (BusinessFailureException exception) {
            markOrderFailed(order, exception.reason(), order.getUserId(), CODE_STATUS_FAILED);
            writeFailure(order.getActivityId(), order.getUserId(), orderRecordMapper.selectById(order.getId()), exception.reason(), activity);
        }
    }

    @Transactional
    public void handleOrderTimeoutClose(OrderTimeoutClosePayload payload) {
        OrderRecordEntity order = orderRecordMapper.findByOrderNo(payload.orderNo());
        if (order == null || !PAY_STATUS_WAIT_PAY.equals(order.getPayStatus()) || isClosed(order) || isSuccess(order)) {
            return;
        }
        ActivityProductEntity activity = loadActivity(order.getActivityId());
        markOrderClosed(order, FailureReason.PAYMENT_TIMEOUT, order.getUserId());
        compensate(order.getActivityId(), order.getUserId());
        writeFailure(order.getActivityId(), order.getUserId(), orderRecordMapper.selectById(order.getId()), FailureReason.PAYMENT_TIMEOUT, activity);
    }

    public OrderCodeView queryOrderCode(String orderNo, Long currentUserId) {
        OrderRecordEntity order = orderRecordMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!order.getUserId().equals(currentUserId)) {
            throw new com.flashsale.common.security.exception.ForbiddenException("无权查看该订单");
        }
        RedeemCodeEntity redeemCode = redeemCodeMapper.findByAssignedOrderId(order.getId());
        return new OrderCodeView(
                order.getOrderNo(),
                order.getActivityId(),
                order.getOrderStatus(),
                order.getPayStatus(),
                order.getCodeStatus(),
                redeemCode == null ? null : redeemCode.getCode(),
                order.getUpdatedAt()
        );
    }

    public OrderDetailView queryOrder(String orderNo, Long currentUserId) {
        OrderRecordEntity order = orderRecordMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!order.getUserId().equals(currentUserId)) {
            throw new com.flashsale.common.security.exception.ForbiddenException("无权查看该订单");
        }
        return new OrderDetailView(
                order.getOrderNo(),
                order.getActivityId(),
                order.getUserId(),
                order.getOrderStatus(),
                order.getPayStatus(),
                order.getCodeStatus(),
                order.getPriceAmount(),
                order.getFailReason(),
                order.getUpdatedAt()
        );
    }

    private ActivityProductEntity loadActivity(Long activityId) {
        return activityProductMapper.selectOne(new LambdaQueryWrapper<ActivityProductEntity>()
                .eq(ActivityProductEntity::getId, activityId)
                .eq(ActivityProductEntity::getIsDeleted, 0)
                .last("limit 1"));
    }

    private OrderRecordEntity createInitOrder(OrderCreatePayload payload, BigDecimal priceAmount, String payStatus) {
        OrderRecordEntity entity = new OrderRecordEntity();
        entity.setOrderNo(orderNoGenerator.nextOrderNo());
        entity.setActivityId(payload.activityId());
        entity.setUserId(payload.userId());
        entity.setRequestId(payload.requestId());
        entity.setPurchaseUniqueKey(payload.purchaseUniqueKey());
        entity.setOrderStatus(ORDER_STATUS_INIT);
        entity.setPayStatus(payStatus);
        entity.setCodeStatus(CODE_STATUS_PENDING);
        entity.setPriceAmount(priceAmount);
        entity.setCreatedBy(payload.userId());
        entity.setUpdatedBy(payload.userId());
        entity.setIsDeleted(0);
        orderRecordMapper.insert(entity);
        return entity;
    }

    private String issueCode(OrderCreatePayload payload, OrderRecordEntity order) {
        ActivityProductEntity activity = loadActivity(payload.activityId());
        if (activity == null) {
            throw new BusinessFailureException(FailureReason.ACTIVITY_NOT_FOUND);
        }
        return issueCode(activity, order);
    }

    private String issueCode(ActivityProductEntity activity, OrderRecordEntity order) {
        RedeemCodeEntity assignedCode = redeemCodeMapper.findByAssignedOrderId(order.getId());
        if (assignedCode != null) {
            return assignedCode.getCode();
        }
        if ("THIRD_PARTY_IMPORTED".equals(activity.getCodeSourceMode())) {
            return assignImportedCode(order);
        }
        return generateSystemCode(order);
    }

    private String assignImportedCode(OrderRecordEntity order) {
        for (int attempt = 0; attempt < IMPORTED_CODE_CLAIM_MAX_ATTEMPTS; attempt++) {
            RedeemCodeEntity candidate = redeemCodeMapper.findFirstAvailableCode(order.getActivityId());
            if (candidate == null) {
                throw new BusinessFailureException(FailureReason.IMPORTED_CODE_UNAVAILABLE);
            }
            LocalDateTime now = LocalDateTime.now(clock);
            int updated = redeemCodeMapper.claimImportedCode(candidate.getId(), order.getUserId(), order.getId(), now);
            if (updated == 1) {
                return candidate.getCode();
            }
        }
        throw new BusinessFailureException(FailureReason.IMPORTED_CODE_UNAVAILABLE);
    }

    private String generateSystemCode(OrderRecordEntity order) {
        for (int attempt = 0; attempt < SYSTEM_CODE_MAX_ATTEMPTS; attempt++) {
            String generatedCode = redeemCodeGenerator.nextCode();
            RedeemCodeEntity entity = new RedeemCodeEntity();
            entity.setActivityId(order.getActivityId());
            entity.setCode(generatedCode);
            entity.setSourceType("SYSTEM_GENERATED");
            entity.setStatus("ASSIGNED");
            entity.setAssignedUserId(order.getUserId());
            entity.setAssignedOrderId(order.getId());
            entity.setAssignedAt(LocalDateTime.now(clock));
            entity.setCreatedBy(order.getUserId());
            entity.setUpdatedBy(order.getUserId());
            entity.setIsDeleted(0);
            try {
                redeemCodeMapper.insert(entity);
                return generatedCode;
            } catch (DataAccessException exception) {
                if (attempt == SYSTEM_CODE_MAX_ATTEMPTS - 1) {
                    throw new BusinessFailureException(FailureReason.SYSTEM_CODE_GENERATION_FAILED);
                }
            }
        }
        throw new BusinessFailureException(FailureReason.SYSTEM_CODE_GENERATION_FAILED);
    }

    private void markOrderPaid(OrderRecordEntity order, Long operatorId) {
        order.setPayStatus(PAY_STATUS_PAID);
        order.setUpdatedBy(operatorId);
        orderRecordMapper.updateById(order);
    }

    private void markOrderSuccess(OrderRecordEntity order, Long operatorId) {
        order.setOrderStatus(ORDER_STATUS_CONFIRMED);
        order.setCodeStatus(CODE_STATUS_ISSUED);
        order.setFailReason(null);
        order.setUpdatedBy(operatorId);
        orderRecordMapper.updateById(order);
    }

    private void markOrderFailed(OrderRecordEntity order, FailureReason reason, Long operatorId, String codeStatus) {
        order.setOrderStatus(ORDER_STATUS_FAILED);
        if (codeStatus != null && !codeStatus.isBlank()) {
            order.setCodeStatus(codeStatus);
        }
        order.setFailReason(reason.storedValue());
        order.setUpdatedBy(operatorId);
        orderRecordMapper.updateById(order);
    }

    private void markOrderClosed(OrderRecordEntity order, FailureReason reason, Long operatorId) {
        order.setOrderStatus(ORDER_STATUS_CLOSED);
        order.setPayStatus(PAY_STATUS_CLOSED);
        order.setFailReason(reason.storedValue());
        order.setUpdatedBy(operatorId);
        orderRecordMapper.updateById(order);
    }

    private void compensateAndWriteFailure(
            OrderCreatePayload payload,
            OrderRecordEntity order,
            FailureReason reason,
            ActivityProductEntity activity
    ) {
        compensate(payload.activityId(), payload.userId());
        writeFailure(payload.activityId(), payload.userId(), order, reason, activity);
    }

    private void compensate(OrderCreatePayload payload) {
        compensate(payload.activityId(), payload.userId());
    }

    private void compensate(Long activityId, Long userId) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        valueOperations.increment(RedisKeys.seckillStock(activityId));
        Long remaining = valueOperations.decrement(RedisKeys.seckillLimit(activityId, userId));
        if (remaining != null && remaining < 0) {
            valueOperations.increment(RedisKeys.seckillLimit(activityId, userId));
        }
    }

    private void writeSuccess(
            Long activityId,
            Long userId,
            OrderRecordEntity order,
            String code,
            ActivityProductEntity activity
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("orderNo", order == null ? "" : order.getOrderNo());
        result.put("message", "抢购成功");
        result.put("code", code == null ? "" : code);
        result.put("updatedAt", LocalDateTime.now(clock).toString());
        writeResult(activityId, userId, result, activity);
    }

    private void writePendingPayment(Long activityId, Long userId, OrderRecordEntity order, ActivityProductEntity activity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "PENDING_PAYMENT");
        result.put("orderNo", order == null ? "" : order.getOrderNo());
        result.put("message", "待支付");
        result.put("code", "");
        result.put("updatedAt", LocalDateTime.now(clock).toString());
        writeResult(activityId, userId, result, activity);
    }

    private void writeFailure(
            Long activityId,
            Long userId,
            OrderRecordEntity order,
            FailureReason reason,
            ActivityProductEntity activity
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "FAIL");
        result.put("orderNo", order == null ? "" : order.getOrderNo());
        result.put("message", reason.userMessage());
        result.put("code", "");
        result.put("updatedAt", LocalDateTime.now(clock).toString());
        writeResult(activityId, userId, result, activity);
    }

    private void writeResult(Long activityId, Long userId, Map<String, Object> result, ActivityProductEntity activity) {
        String key = RedisKeys.seckillResult(activityId, userId);
        stringRedisTemplate.opsForHash().putAll(key, result);
        stringRedisTemplate.expire(key, ttl(activity == null ? null : activity.getEndTime()));
    }

    private Duration ttl(LocalDateTime endTime) {
        if (endTime == null) {
            return Duration.ofHours(1);
        }
        Duration ttl = Duration.between(LocalDateTime.now(clock), endTime.plusHours(24));
        if (ttl.isNegative() || ttl.isZero()) {
            return Duration.ofHours(1);
        }
        return ttl;
    }

    private boolean isSuccess(OrderRecordEntity order) {
        return ORDER_STATUS_CONFIRMED.equals(order.getOrderStatus()) && CODE_STATUS_ISSUED.equals(order.getCodeStatus());
    }

    private boolean isFailed(OrderRecordEntity order) {
        return ORDER_STATUS_FAILED.equals(order.getOrderStatus());
    }

    private boolean isClosed(OrderRecordEntity order) {
        return ORDER_STATUS_CLOSED.equals(order.getOrderStatus()) || PAY_STATUS_CLOSED.equals(order.getPayStatus());
    }

    private String assignedCodeValue(OrderRecordEntity order) {
        RedeemCodeEntity redeemCode = redeemCodeMapper.findByAssignedOrderId(order.getId());
        return redeemCode == null ? null : redeemCode.getCode();
    }

    private static class BusinessFailureException extends RuntimeException {

        private final FailureReason reason;

        private BusinessFailureException(FailureReason reason) {
            this.reason = reason;
        }

        private FailureReason reason() {
            return reason;
        }
    }

    public record OrderCodeView(
            String orderNo,
            Long activityId,
            String orderStatus,
            String payStatus,
            String codeStatus,
            String code,
            LocalDateTime updatedAt
    ) {
    }

    public record OrderDetailView(
            String orderNo,
            Long activityId,
            Long userId,
            String orderStatus,
            String payStatus,
            String codeStatus,
            BigDecimal priceAmount,
            String failReason,
            LocalDateTime updatedAt
    ) {
    }

    enum FailureReason {
        ACTIVITY_NOT_FOUND("ACTIVITY_NOT_FOUND", "活动不存在"),
        IMPORTED_CODE_UNAVAILABLE("IMPORTED_CODE_UNAVAILABLE", "兑换码不足"),
        SYSTEM_CODE_GENERATION_FAILED("SYSTEM_CODE_GENERATION_FAILED", "系统发码失败"),
        ORDER_CREATE_FAILED("ORDER_CREATE_FAILED", "订单处理失败"),
        PAYMENT_TIMEOUT("PAYMENT_TIMEOUT", "支付超时，订单已关闭");

        private final String storedValue;
        private final String userMessage;

        FailureReason(String storedValue, String userMessage) {
            this.storedValue = storedValue;
            this.userMessage = userMessage;
        }

        String storedValue() {
            return storedValue;
        }

        String userMessage() {
            return userMessage;
        }

        static FailureReason fromStoredValue(String value) {
            if (value == null || value.isBlank()) {
                return ORDER_CREATE_FAILED;
            }
            for (FailureReason reason : values()) {
                if (reason.storedValue.equalsIgnoreCase(value)) {
                    return reason;
                }
            }
            return ORDER_CREATE_FAILED;
        }
    }
}
