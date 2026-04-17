package com.flashsale.activity.domain;

/**
 * 活动限购类型。
 */
public enum PurchaseLimitType {
    /** 单次限购（每个用户仅允许一次购买机会） */
    SINGLE,
    /** 多次可购（每个用户允许多次购买） */
    MULTI
}
