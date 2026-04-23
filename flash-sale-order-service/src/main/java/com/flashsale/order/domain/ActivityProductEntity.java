package com.flashsale.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("activity_product")
public class ActivityProductEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("code_source_mode")
    private String codeSourceMode;

    @TableField("price_amount")
    private BigDecimal priceAmount;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("created_by")
    private Long createdBy;

    @TableField("is_deleted")
    private Integer isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodeSourceMode() {
        return codeSourceMode;
    }

    public void setCodeSourceMode(String codeSourceMode) {
        this.codeSourceMode = codeSourceMode;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount) {
        this.priceAmount = priceAmount;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
