package com.flashsale.order.service;

import com.flashsale.order.dto.response.PublisherDashboardResponse;
import com.flashsale.order.dto.response.PublisherDashboardResponse.ActivityPerformanceItem;
import com.flashsale.order.dto.response.PublisherDashboardResponse.Insights;
import com.flashsale.order.dto.response.PublisherDashboardResponse.Summary;
import com.flashsale.order.dto.response.PublisherDashboardResponse.Trend;
import com.flashsale.order.dto.response.PublisherDashboardResponse.TrendBucket;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PublisherDashboardService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public PublisherDashboardService(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public PublisherDashboardResponse getDashboard(Long publisherId, String granularity) {
        DashboardGranularity resolvedGranularity = DashboardGranularity.from(granularity);
        PeriodRange currentPeriod = resolvedGranularity.currentPeriod(LocalDate.now(clock));
        PeriodRange previousPeriod = currentPeriod.previous();

        SummaryMetrics currentSummary = querySummaryMetrics(publisherId, currentPeriod);
        SummaryMetrics previousSummary = querySummaryMetrics(publisherId, previousPeriod);
        List<ActivityStockRow> activityStocks = queryActivityStocks(publisherId);
        List<ActivityPeriodMetrics> currentActivities = queryActivityMetrics(publisherId, currentPeriod);
        List<ActivityPeriodMetrics> previousActivities = queryActivityMetrics(publisherId, previousPeriod);

        Summary summary = buildSummary(currentSummary, previousSummary, activityStocks);
        Trend trend = buildTrend(publisherId, resolvedGranularity, currentPeriod, activityStocks);
        List<ActivityPerformanceItem> activityPerformance = buildActivityPerformance(activityStocks, currentActivities, previousActivities);
        Insights insights = buildInsights(activityStocks, currentActivities);

        return new PublisherDashboardResponse(summary, trend, activityPerformance, insights);
    }

    private Summary buildSummary(SummaryMetrics current, SummaryMetrics previous, List<ActivityStockRow> activityStocks) {
        long inventoryConsumed = activityStocks.stream()
                .mapToLong(ActivityStockRow::consumedStock)
                .sum();
        long inventoryTotal = activityStocks.stream()
                .mapToLong(ActivityStockRow::totalStock)
                .sum();
        BigDecimal inventoryConsumptionRate = ratio(inventoryConsumed, inventoryTotal);
        long highConsumptionCount = activityStocks.stream()
                .filter(activity -> activity.inventoryConsumptionRate().compareTo(new BigDecimal("0.8")) >= 0)
                .count();

        return new Summary(
                scaleMoney(current.revenue()),
                changeRate(current.revenue(), previous.revenue()),
                averageOrderValue(current.revenue(), current.paidOrders()),
                current.totalOrders(),
                changeRate(current.totalOrders(), previous.totalOrders()),
                current.paidOrders(),
                changeRate(current.paidOrders(), previous.paidOrders()),
                ratio(current.paidOrders(), current.totalOrders()),
                inventoryConsumed,
                inventoryTotal,
                inventoryConsumptionRate,
                highConsumptionCount,
                current.pendingCompensations()
        );
    }

    private Trend buildTrend(Long publisherId, DashboardGranularity granularity, PeriodRange period, List<ActivityStockRow> activityStocks) {
        Map<String, TrendBucketAccumulator> accumulators = new LinkedHashMap<>();
        for (PeriodRange bucketRange : granularity.buckets(period)) {
            accumulators.put(bucketRange.key(), new TrendBucketAccumulator(bucketRange));
        }

        List<TrendOrderRow> rows = queryTrendOrderRows(publisherId, period);
        for (TrendOrderRow row : rows) {
            PeriodRange bucket = granularity.bucketFor(row.createdDate());
            TrendBucketAccumulator accumulator = accumulators.get(bucket.key());
            if (accumulator != null) {
                accumulator.totalOrders += 1;
                if ("PAID".equals(row.payStatus())) {
                    accumulator.paidOrders += 1;
                }
                if ("CONFIRMED".equals(row.orderStatus())) {
                    accumulator.revenue = accumulator.revenue.add(row.priceAmount());
                }
            }
        }

        BigDecimal overallInventoryRate = ratio(
                activityStocks.stream().mapToLong(ActivityStockRow::consumedStock).sum(),
                activityStocks.stream().mapToLong(ActivityStockRow::totalStock).sum()
        );

        List<TrendBucket> buckets = accumulators.values().stream()
                .map(acc -> new TrendBucket(
                        acc.range.label(granularity),
                        acc.range.start().format(DATE_FORMAT),
                        acc.range.end().format(DATE_FORMAT),
                        scaleMoney(acc.revenue),
                        acc.totalOrders,
                        acc.paidOrders,
                        overallInventoryRate
                ))
                .toList();

        return new Trend(
                granularity.value(),
                period.label(),
                buckets
        );
    }

    private List<ActivityPerformanceItem> buildActivityPerformance(List<ActivityStockRow> activityStocks,
                                                                   List<ActivityPeriodMetrics> currentActivities,
                                                                   List<ActivityPeriodMetrics> previousActivities) {
        Map<Long, ActivityStockRow> stockMap = new LinkedHashMap<>();
        for (ActivityStockRow stock : activityStocks) {
            stockMap.put(stock.activityId(), stock);
        }

        Map<Long, ActivityPeriodMetrics> currentMap = indexByActivity(currentActivities);
        Map<Long, ActivityPeriodMetrics> previousMap = indexByActivity(previousActivities);

        List<ActivityPerformanceItem> items = new ArrayList<>();
        for (ActivityStockRow stock : activityStocks) {
            ActivityPeriodMetrics current = currentMap.getOrDefault(stock.activityId(), ActivityPeriodMetrics.empty(stock.activityId(), stock.title()));
            ActivityPeriodMetrics previous = previousMap.getOrDefault(stock.activityId(), ActivityPeriodMetrics.empty(stock.activityId(), stock.title()));

            items.add(new ActivityPerformanceItem(
                    stock.activityId(),
                    stock.title(),
                    stock.phase(),
                    scaleMoney(current.revenue()),
                    changeRate(current.revenue(), previous.revenue()),
                    current.totalOrders(),
                    changeRate(current.totalOrders(), previous.totalOrders()),
                    current.paidOrders(),
                    ratio(current.paidOrders(), current.totalOrders()),
                    stock.inventoryConsumptionRate()
            ));
        }

        items.sort(Comparator
                .comparing(ActivityPerformanceItem::revenue, Comparator.reverseOrder())
                .thenComparing(ActivityPerformanceItem::paidOrders, Comparator.reverseOrder())
                .thenComparing(ActivityPerformanceItem::activityId));
        return items;
    }

    private Insights buildInsights(List<ActivityStockRow> activityStocks, List<ActivityPeriodMetrics> currentActivities) {
        long highConsumption = activityStocks.stream()
                .filter(activity -> activity.inventoryConsumptionRate().compareTo(new BigDecimal("0.8")) >= 0)
                .count();
        long mediumConsumption = activityStocks.stream()
                .filter(activity -> activity.inventoryConsumptionRate().compareTo(new BigDecimal("0.3")) >= 0)
                .filter(activity -> activity.inventoryConsumptionRate().compareTo(new BigDecimal("0.8")) < 0)
                .count();
        long lowConsumption = activityStocks.size() - highConsumption - mediumConsumption;

        List<String> messages = new ArrayList<>();

        long highRevenueHighConsumption = currentActivities.stream()
                .filter(activity -> activity.revenue().compareTo(BigDecimal.ZERO) > 0)
                .filter(activity -> {
                    ActivityStockRow stock = activityStocks.stream()
                            .filter(item -> Objects.equals(item.activityId(), activity.activityId()))
                            .findFirst()
                            .orElse(null);
                    return stock != null && stock.inventoryConsumptionRate().compareTo(new BigDecimal("0.9")) >= 0;
                })
                .count();
        if (highRevenueHighConsumption > 0) {
            messages.add(highRevenueHighConsumption + " 个高营收活动库存消耗超过 90%，需关注供给风险");
        }

        if (messages.isEmpty()) {
            messages.add("当前周期暂无经营数据");
        }

        return new Insights(highConsumption, mediumConsumption, lowConsumption, messages);
    }

    private SummaryMetrics querySummaryMetrics(Long publisherId, PeriodRange period) {
        return jdbcTemplate.queryForObject(
                """
                select
                  coalesce(sum(case when o.order_status = 'CONFIRMED' then o.price_amount else 0 end), 0) as revenue,
                  count(*) as total_orders,
                  coalesce(sum(case when o.pay_status = 'PAID' then 1 else 0 end), 0) as paid_orders
                from order_record o
                join activity_product a on a.id = o.activity_id
                where a.created_by = ?
                  and a.is_deleted = 0
                  and o.is_deleted = 0
                  and o.created_at >= ?
                  and o.created_at < ?
                """,
                (rs, rowNum) -> new SummaryMetrics(
                        nullableBigDecimal(rs, "revenue"),
                        rs.getLong("total_orders"),
                        rs.getLong("paid_orders"),
                        queryPendingCompensations()
                ),
                publisherId,
                period.startDateTime(),
                period.endExclusiveDateTime()
        );
    }

    private List<ActivityStockRow> queryActivityStocks(Long publisherId) {
        return jdbcTemplate.query(
                """
                select
                  a.id,
                  a.title,
                  a.total_stock,
                  a.available_stock,
                  case
                    when a.publish_status = 'OFFLINE' then 'OFFLINE'
                    when a.publish_status = 'UNPUBLISHED' then 'UNPUBLISHED'
                    when a.start_time > ? then 'PREVIEW'
                    when a.end_time >= ? then 'ONGOING'
                    else 'ENDED'
                  end as phase
                from activity_product a
                where a.created_by = ?
                  and a.is_deleted = 0
                order by a.id asc
                """,
                (rs, rowNum) -> mapActivityStock(rs),
                LocalDateTime.now(clock),
                LocalDateTime.now(clock),
                publisherId
        );
    }

    private List<ActivityPeriodMetrics> queryActivityMetrics(Long publisherId, PeriodRange period) {
        return jdbcTemplate.query(
                """
                select
                  a.id as activity_id,
                  a.title,
                  coalesce(sum(case when o.order_status = 'CONFIRMED' then o.price_amount else 0 end), 0) as revenue,
                  count(o.id) as total_orders,
                  coalesce(sum(case when o.pay_status = 'PAID' then 1 else 0 end), 0) as paid_orders
                from activity_product a
                left join order_record o
                  on o.activity_id = a.id
                 and o.is_deleted = 0
                 and o.created_at >= ?
                 and o.created_at < ?
                where a.created_by = ?
                  and a.is_deleted = 0
                group by a.id, a.title
                order by a.id asc
                """,
                (rs, rowNum) -> new ActivityPeriodMetrics(
                        rs.getLong("activity_id"),
                        rs.getString("title"),
                        nullableBigDecimal(rs, "revenue"),
                        rs.getLong("total_orders"),
                        rs.getLong("paid_orders")
                ),
                period.startDateTime(),
                period.endExclusiveDateTime(),
                publisherId
        );
    }

    private List<TrendOrderRow> queryTrendOrderRows(Long publisherId, PeriodRange period) {
        return jdbcTemplate.query(
                """
                select
                  date(o.created_at) as created_date,
                  o.order_status,
                  o.pay_status,
                  o.price_amount
                from order_record o
                join activity_product a on a.id = o.activity_id
                where a.created_by = ?
                  and a.is_deleted = 0
                  and o.is_deleted = 0
                  and o.created_at >= ?
                  and o.created_at < ?
                order by o.created_at asc
                """,
                (rs, rowNum) -> new TrendOrderRow(
                        rs.getDate("created_date").toLocalDate(),
                        rs.getString("order_status"),
                        rs.getString("pay_status"),
                        nullableBigDecimal(rs, "price_amount")
                ),
                publisherId,
                period.startDateTime(),
                period.endExclusiveDateTime()
        );
    }

    private long queryPendingCompensations() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from compensation_record where status = 'PENDING' and is_deleted = 0",
                Long.class
        );
        return count == null ? 0 : count;
    }

    private Map<Long, ActivityPeriodMetrics> indexByActivity(List<ActivityPeriodMetrics> rows) {
        Map<Long, ActivityPeriodMetrics> map = new LinkedHashMap<>();
        for (ActivityPeriodMetrics row : rows) {
            map.put(row.activityId(), row);
        }
        return map;
    }

    private ActivityStockRow mapActivityStock(ResultSet rs) throws SQLException {
        long totalStock = rs.getLong("total_stock");
        long availableStock = rs.getLong("available_stock");
        long consumed = Math.max(0, totalStock - availableStock);
        return new ActivityStockRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("phase"),
                totalStock,
                availableStock,
                consumed,
                ratio(consumed, totalStock)
        );
    }

    private BigDecimal changeRate(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current.compareTo(BigDecimal.ZERO) == 0) {
                return ZERO;
            }
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal changeRate(long current, long previous) {
        return changeRate(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal averageOrderValue(BigDecimal revenue, long paidOrders) {
        if (paidOrders <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return revenue.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullableBigDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }

    private enum DashboardGranularity {
        DAY("day"),
        WEEK("week"),
        MONTH("month");

        private final String value;

        DashboardGranularity(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }

        static DashboardGranularity from(String raw) {
            if (raw == null || raw.isBlank()) {
                return WEEK;
            }
            for (DashboardGranularity candidate : values()) {
                if (candidate.value.equalsIgnoreCase(raw)) {
                    return candidate;
                }
            }
            return WEEK;
        }

        PeriodRange currentPeriod(LocalDate today) {
            return switch (this) {
                case DAY -> {
                    LocalDate end = today;
                    LocalDate start = today.minusDays(6);
                    yield new PeriodRange(start, end);
                }
                case WEEK -> {
                    LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    LocalDate start = currentWeekStart.minusWeeks(7);
                    LocalDate end = currentWeekStart.plusDays(6);
                    yield new PeriodRange(start, end);
                }
                case MONTH -> {
                    YearMonth currentMonth = YearMonth.from(today);
                    YearMonth startMonth = currentMonth.minusMonths(5);
                    LocalDate start = startMonth.atDay(1);
                    LocalDate end = currentMonth.atEndOfMonth();
                    yield new PeriodRange(start, end);
                }
            };
        }

        List<PeriodRange> buckets(PeriodRange period) {
            List<PeriodRange> buckets = new ArrayList<>();
            switch (this) {
                case DAY -> {
                    LocalDate date = period.start();
                    while (!date.isAfter(period.end())) {
                        buckets.add(new PeriodRange(date, date));
                        date = date.plusDays(1);
                    }
                }
                case WEEK -> {
                    LocalDate date = period.start();
                    while (!date.isAfter(period.end())) {
                        PeriodRange bucket = new PeriodRange(date, date.plusDays(6));
                        buckets.add(bucket);
                        date = date.plusWeeks(1);
                    }
                }
                case MONTH -> {
                    LocalDate date = period.start().withDayOfMonth(1);
                    while (!date.isAfter(period.end())) {
                        YearMonth month = YearMonth.from(date);
                        buckets.add(new PeriodRange(month.atDay(1), month.atEndOfMonth()));
                        date = date.plusMonths(1);
                    }
                }
            }
            return buckets;
        }

        PeriodRange bucketFor(LocalDate date) {
            return switch (this) {
                case DAY -> new PeriodRange(date, date);
                case WEEK -> {
                    LocalDate start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    yield new PeriodRange(start, start.plusDays(6));
                }
                case MONTH -> {
                    YearMonth month = YearMonth.from(date);
                    yield new PeriodRange(month.atDay(1), month.atEndOfMonth());
                }
            };
        }
    }

    private record PeriodRange(LocalDate start, LocalDate end) {
        String label() {
            return start.format(DATE_FORMAT) + " 至 " + end.format(DATE_FORMAT);
        }

        String key() {
            return start + "_" + end;
        }

        LocalDateTime startDateTime() {
            return start.atStartOfDay();
        }

        LocalDateTime endExclusiveDateTime() {
            return end.plusDays(1).atStartOfDay();
        }

        PeriodRange previous() {
            long days = end.toEpochDay() - start.toEpochDay() + 1;
            LocalDate previousEnd = start.minusDays(1);
            LocalDate previousStart = previousEnd.minusDays(days - 1);
            return new PeriodRange(previousStart, previousEnd);
        }

        String label(DashboardGranularity granularity) {
            return switch (granularity) {
                case DAY -> start.format(DateTimeFormatter.ofPattern("MM-dd"));
                case WEEK -> start.format(DateTimeFormatter.ofPattern("MM-dd")) + " 至 " +
                        end.format(DateTimeFormatter.ofPattern("MM-dd"));
                case MONTH -> YearMonth.from(start).toString();
            };
        }
    }

    private record SummaryMetrics(
            BigDecimal revenue,
            long totalOrders,
            long paidOrders,
            long pendingCompensations
    ) {}

    private record ActivityStockRow(
            Long activityId,
            String title,
            String phase,
            long totalStock,
            long availableStock,
            long consumedStock,
            BigDecimal inventoryConsumptionRate
    ) {}

    private record ActivityPeriodMetrics(
            Long activityId,
            String title,
            BigDecimal revenue,
            long totalOrders,
            long paidOrders
    ) {
        static ActivityPeriodMetrics empty(Long activityId, String title) {
            return new ActivityPeriodMetrics(activityId, title, BigDecimal.ZERO, 0, 0);
        }
    }

    private record TrendOrderRow(
            LocalDate createdDate,
            String orderStatus,
            String payStatus,
            BigDecimal priceAmount
    ) {}

    private static class TrendBucketAccumulator {
        private final PeriodRange range;
        private BigDecimal revenue = BigDecimal.ZERO;
        private long totalOrders;
        private long paidOrders;

        private TrendBucketAccumulator(PeriodRange range) {
            this.range = range;
        }
    }
}
