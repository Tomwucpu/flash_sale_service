package com.flashsale.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("activity_product")
public class ActivityProductEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("code_source_mode")
    private String codeSourceMode;

    @TableField("end_time")
    private LocalDateTime endTime;

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

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
