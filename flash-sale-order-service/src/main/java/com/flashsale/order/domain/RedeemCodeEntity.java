package com.flashsale.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 兑换码实体类
 * 用于映射数据库中的兑换码表，管理兑换码的生成、分配与使用状态
 */
@TableName("redeem_code")
public class RedeemCodeEntity {

    /** 主键自增ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的秒杀活动ID */
    @TableField("activity_id")
    private Long activityId;

    /** 兑换码具体内容 */
    private String code;

    /** 来源类型 (如: SYSTEM生成、外部导入等) */
    @TableField("source_type")
    private String sourceType;

    /** 批次号 (用于批量生成或导入任务的追踪) */
    @TableField("batch_no")
    private String batchNo;

    /** 兑换码状态 (如: AVAILABLE-可用, ASSIGNED-已分配, USED-已核销) */
    private String status;

    /** 分配目标用户ID (未分配时为空) */
    @TableField("assigned_user_id")
    private Long assignedUserId;

    /** 绑定的订单ID (未分配时为空) */
    @TableField("assigned_order_id")
    private Long assignedOrderId;

    /** 发放/分配时间 */
    @TableField("assigned_at")
    private LocalDateTime assignedAt;

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

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public Long getAssignedOrderId() {
        return assignedOrderId;
    }

    public void setAssignedOrderId(Long assignedOrderId) {
        this.assignedOrderId = assignedOrderId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
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
