package com.flashsale.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单记录实体类
 * 用于映射数据库中的订单核心交易数据表
 */
@TableName("order_record")
public class OrderRecordEntity {

    /** 主键自增ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务订单号 (对外展示和流转使用) */
    @TableField("order_no")
    private String orderNo;

    /** 关联的秒杀活动ID */
    @TableField("activity_id")
    private Long activityId;

    /** 下单用户ID */
    @TableField("user_id")
    private Long userId;

    /** 全链路请求追踪ID (防重/排查日志) */
    @TableField("request_id")
    private String requestId;

    /** 购买操作防重唯一键 (例如 userId + activityId) */
    @TableField("purchase_unique_key")
    private String purchaseUniqueKey;

    /** 订单整体状态 (如: PENDING, SUCCESS, CANCELED) */
    @TableField("order_status")
    private String orderStatus;

    /** 支付状态 (如: UNPAID, PAID, REFUNDED) */
    @TableField("pay_status")
    private String payStatus;

    /** 兑换码发放状态 (如: UNISSUED, ISSUED) */
    @TableField("code_status")
    private String codeStatus;

    /** 订单实际需支付金额 */
    @TableField("price_amount")
    private BigDecimal priceAmount;

    /** 订单失败或取消原因 */
    @TableField("fail_reason")
    private String failReason;

    /** 最后更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 创建人ID */
    @TableField("created_by")
    private Long createdBy;

    /** 更新人ID */
    @TableField("updated_by")
    private Long updatedBy;

    /** 逻辑删除标志 (通常0代表正常，1代表删除) */
    @TableField("is_deleted")
    private Integer isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPurchaseUniqueKey() {
        return purchaseUniqueKey;
    }

    public void setPurchaseUniqueKey(String purchaseUniqueKey) {
        this.purchaseUniqueKey = purchaseUniqueKey;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public String getCodeStatus() {
        return codeStatus;
    }

    public void setCodeStatus(String codeStatus) {
        this.codeStatus = codeStatus;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount) {
        this.priceAmount = priceAmount;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
