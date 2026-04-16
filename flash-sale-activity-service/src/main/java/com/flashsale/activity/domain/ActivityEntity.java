package com.flashsale.activity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("activity_product")
public class ActivityEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    @TableField("cover_url")
    private String coverUrl;

    @TableField("total_stock")
    private Integer totalStock;

    @TableField("available_stock")
    private Integer availableStock;

    @TableField("price_amount")
    private BigDecimal priceAmount;

    @TableField("need_payment")
    private Boolean needPayment;

    @TableField("purchase_limit_type")
    private String purchaseLimitType;

    @TableField("purchase_limit_count")
    private Integer purchaseLimitCount;

    @TableField("code_source_mode")
    private String codeSourceMode;

    @TableField("publish_mode")
    private String publishMode;

    @TableField("publish_status")
    private String publishStatus;

    @TableField("publish_time")
    private LocalDateTime publishTime;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    private Integer version;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField("is_deleted")
    private Integer isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount) {
        this.priceAmount = priceAmount;
    }

    public Boolean getNeedPayment() {
        return needPayment;
    }

    public void setNeedPayment(Boolean needPayment) {
        this.needPayment = needPayment;
    }

    public String getPurchaseLimitType() {
        return purchaseLimitType;
    }

    public void setPurchaseLimitType(String purchaseLimitType) {
        this.purchaseLimitType = purchaseLimitType;
    }

    public Integer getPurchaseLimitCount() {
        return purchaseLimitCount;
    }

    public void setPurchaseLimitCount(Integer purchaseLimitCount) {
        this.purchaseLimitCount = purchaseLimitCount;
    }

    public String getCodeSourceMode() {
        return codeSourceMode;
    }

    public void setCodeSourceMode(String codeSourceMode) {
        this.codeSourceMode = codeSourceMode;
    }

    public String getPublishMode() {
        return publishMode;
    }

    public void setPublishMode(String publishMode) {
        this.publishMode = publishMode;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
