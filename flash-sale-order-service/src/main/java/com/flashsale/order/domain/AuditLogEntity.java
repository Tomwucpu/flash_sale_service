package com.flashsale.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 审计日志实体类
 * 用于记录核心业务数据变更和关键操作轨迹
 */
@TableName("audit_log")
public class AuditLogEntity {

    /** 主键自增ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务分类 (诸如: ORDER, PAYMENT) */
    @TableField("biz_type")
    private String bizType;

    /** 业务主键标识 (诸如: 订单号、支付流水号) */
    @TableField("biz_key")
    private String bizKey;

    /** 具体操作行为 (诸如: CREATE, UPDATE, CANCEL) */
    private String operation;

    /** 执行该操作的用户ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 全链路追踪的 Request ID */
    @TableField("request_id")
    private String requestId;

    /** 操作前后的数据快照或详情 (JSON格式存储) */
    @TableField("detail_json")
    private String detailJson;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
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

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizKey() {
        return bizKey;
    }

    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
