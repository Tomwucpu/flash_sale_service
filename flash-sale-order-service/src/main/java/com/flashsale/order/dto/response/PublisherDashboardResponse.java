package com.flashsale.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublisherDashboardResponse(
        Summary summary,
        Trend trend,
        List<ActivityPerformanceItem> activityPerformance,
        Insights insights
) {
    public record Summary(
            BigDecimal revenue,
            BigDecimal revenueChangeRate,
            BigDecimal avgOrderValue,
            long totalOrders,
            BigDecimal totalOrdersChangeRate,
            long paidOrders,
            BigDecimal paidOrdersChangeRate,
            BigDecimal paidOrderRate,
            long inventoryConsumed,
            long inventoryTotal,
            BigDecimal inventoryConsumptionRate,
            long highConsumptionActivityCount,
            long pendingCompensations
    ) {}

    public record Trend(
            String granularity,
            String periodLabel,
            List<TrendBucket> buckets
    ) {}

    public record TrendBucket(
            String label,
            String startDate,
            String endDate,
            BigDecimal revenue,
            long totalOrders,
            long paidOrders,
            BigDecimal inventoryConsumptionRate
    ) {}

    public record ActivityPerformanceItem(
            Long activityId,
            String title,
            String phase,
            BigDecimal revenue,
            BigDecimal revenueChangeRate,
            long totalOrders,
            BigDecimal totalOrdersChangeRate,
            long paidOrders,
            BigDecimal paidOrderRate,
            BigDecimal inventoryConsumptionRate
    ) {}

    public record Insights(
            long highConsumptionCount,
            long mediumConsumptionCount,
            long lowConsumptionCount,
            List<String> messages
    ) {}
}
