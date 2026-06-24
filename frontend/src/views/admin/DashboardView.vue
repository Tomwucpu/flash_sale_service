<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { AlertTriangle, BarChart3, Box, ChartColumn, Clock3, TrendingUp } from 'lucide-vue-next'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api/dashboard'
import { ApiClientError } from '@/api/request'
import type { DashboardGranularity, PublisherDashboard } from '@/types'

type ChangeTone = 'positive' | 'danger' | 'warning' | 'neutral'
type InventoryTone = 'info' | 'warning' | 'danger'
type HighlightTone = ChangeTone

const router = useRouter()
const loading = ref(false)
const granularity = ref<DashboardGranularity>('week')
const dashboard = ref<PublisherDashboard | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null

const granularityOptions = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' },
] as const

const summary = computed(() => dashboard.value?.summary ?? null)
const trend = computed(() => dashboard.value?.trend ?? null)
const activityPerformance = computed(() => dashboard.value?.activityPerformance ?? [])
const insights = computed(() => dashboard.value?.insights ?? null)

const summaryText = computed(() => {
  if (!summary.value) {
    return '正在加载经营摘要'
  }

  return `本周期营收 ${formatCurrency(summary.value.revenue)}，较上周期 ${formatChange(summary.value.revenueChangeRate)}`
})

const showCompensationHint = computed(() => (summary.value?.pendingCompensations ?? 0) > 0)

const heroHighlights = computed(() => {
  if (!summary.value) {
    return []
  }

  return [
    {
      label: '营收环比',
      value: formatChange(summary.value.revenueChangeRate),
      tone: changeTone(summary.value.revenueChangeRate),
    },
    {
      label: '支付转化',
      value: formatPercent(summary.value.paidOrderRate),
      tone: 'neutral' as HighlightTone,
    },
    {
      label: '待处理补偿',
      value: `${summary.value.pendingCompensations} 条`,
      tone: summary.value.pendingCompensations > 0 ? ('warning' as HighlightTone) : ('positive' as HighlightTone),
    },
  ]
})

const trendSummary = computed(() => {
  if (!trend.value?.buckets.length) {
    return '当前周期暂无趋势数据'
  }

  const topBucket = [...trend.value.buckets].sort((a, b) => b.revenue - a.revenue)[0]
  const revenueChangeText = summary.value ? formatChange(summary.value.revenueChangeRate) : '0.0%'
  return `${topBucket.label} 出现营收高点，整体营收较上周期 ${revenueChangeText}`
})

const inventorySummaryText = computed(() => {
  if (!summary.value) {
    return '--'
  }

  if (summary.value.highConsumptionActivityCount > 0) {
    return `高消耗活动 ${summary.value.highConsumptionActivityCount} 个`
  }

  if (summary.value.inventoryConsumptionRate >= 0.3) {
    return '库存消耗平稳，可持续观察'
  }

  return '库存承接宽松，可继续放量'
})

async function loadDashboard() {
  loading.value = true
  try {
    dashboard.value = await dashboardApi.getPublisher(granularity.value)
    await nextTick()
    renderTrendChart()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '经营看板加载失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function formatCurrency(value: number) {
  return `¥${value.toFixed(2)}`
}

function formatPercent(value: number, digits = 1) {
  return `${(value * 100).toFixed(digits)}%`
}

function formatProgressValue(value: number) {
  return (value * 100).toFixed(2)
}

function formatChange(value: number) {
  if (value === 0) return '0.0%'
  if (value === 1) return '新增'
  const sign = value > 0 ? '+' : ''
  return `${sign}${(value * 100).toFixed(1)}%`
}

function changeTone(value: number): ChangeTone {
  if (value === 1) return 'positive'
  if (value > 0) return 'positive'
  if (value < 0) return 'danger'
  return 'neutral'
}

function phaseLabel(phase: string) {
  const labels: Record<string, string> = {
    PREVIEW: '预热中',
    ONGOING: '进行中',
    ENDED: '已结束',
    UNPUBLISHED: '未发布',
    OFFLINE: '已下线',
  }
  return labels[phase] ?? phase
}

function inventoryTone(rate: number): InventoryTone {
  if (rate >= 0.8) return 'danger'
  if (rate >= 0.3) return 'warning'
  return 'info'
}

function inventorySummaryTone(rate: number, highConsumptionCount: number): ChangeTone {
  if (rate >= 0.8) return 'danger'
  if (highConsumptionCount > 0 || rate >= 0.3) return 'warning'
  return 'positive'
}

function inventoryProgressColor(tone: InventoryTone) {
  const colors: Record<InventoryTone, string> = {
    info: 'var(--inventory-info)',
    warning: 'var(--inventory-warning)',
    danger: 'var(--inventory-danger)',
  }

  return colors[tone]
}

function phaseTone(phase: string) {
  const tones: Record<string, string> = {
    PREVIEW: 'preview',
    ONGOING: 'ongoing',
    ENDED: 'ended',
    UNPUBLISHED: 'draft',
    OFFLINE: 'offline',
  }

  return tones[phase] ?? 'default'
}

function inventoryInsightCopy(kind: 'high' | 'medium' | 'low') {
  if (kind === 'high') return '接近售罄，需关注库存承接'
  if (kind === 'medium') return '消耗稳定，可持续观察'
  return '转化偏弱，建议检查活动吸引力'
}

function messageTone(message: string): ChangeTone {
  if (/(补偿|风险|超过|偏低|不足|异常)/.test(message)) return 'warning'
  if (/(增长|提升|稳定|改善)/.test(message)) return 'positive'
  return 'neutral'
}

function goActivity(activityId: number) {
  router.push(`/admin/activities/${activityId}`)
}

function renderTrendChart() {
  if (!trendChartRef.value || !trend.value) {
    return
  }

  trendChart?.dispose()
  trendChart = echarts.init(trendChartRef.value)

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: Array<{ dataIndex: number }>) => {
        const bucket = trend.value?.buckets[params[0]?.dataIndex ?? 0]
        if (!bucket) return ''
        return [
          bucket.label,
          `营收 ${formatCurrency(bucket.revenue)}`,
          `支付订单 ${bucket.paidOrders}`,
          `总订单 ${bucket.totalOrders}`,
          `库存消耗率 ${formatPercent(bucket.inventoryConsumptionRate)}`,
        ].join('<br/>')
      },
    },
    grid: { left: 24, right: 24, top: 64, bottom: 40, containLabel: true },
    legend: { top: 28, data: ['支付订单', '营收'] },
    xAxis: {
      type: 'category',
      data: trend.value.buckets.map((bucket) => bucket.label),
      axisLabel: { interval: 0 },
    },
    yAxis: [
      { type: 'value', name: '订单', minInterval: 1 },
      {
        type: 'value',
        name: '营收',
        axisLabel: {
          formatter: (value: number) => `¥${value}`,
        },
      },
    ],
    series: [
      {
        name: '支付订单',
        type: 'bar',
        data: trend.value.buckets.map((bucket) => bucket.paidOrders),
        barMaxWidth: 28,
        itemStyle: { color: '#2563eb' },
      },
      {
        name: '营收',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: trend.value.buckets.map((bucket) => bucket.revenue),
        itemStyle: { color: '#059669' },
        lineStyle: { color: '#059669', width: 2 },
      },
    ],
  })
}

function handleResize() {
  trendChart?.resize()
}

watch(granularity, async () => {
  await loadDashboard()
})

onMounted(async () => {
  await loadDashboard()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})

defineExpose({
  formatProgressValue,
  inventoryTone,
  inventoryProgressColor,
})
</script>

<template>
  <div class="page-shell">
    <section class="page-header page-header--blue dashboard-hero">
      <div class="dashboard-hero__main">
        <div class="eyebrow">Publisher Dashboard</div>
        <h1 class="dashboard-title">经营看板</h1>
        <p class="dashboard-copy">{{ summaryText }}</p>
        <div class="hero-highlights">
          <span
            v-for="item in heroHighlights"
            :key="item.label"
            class="hero-highlight"
            :data-tone="item.tone"
            data-testid="hero-highlight"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </span>
        </div>
      </div>
      <div class="dashboard-hero__controls">
        <el-segmented v-model="granularity" :options="granularityOptions" />
        <div class="hero-meta">
          <div class="hero-meta__item">
            <Clock3 :size="15" />
            <span>{{ trend?.periodLabel ?? '--' }}</span>
          </div>
          <div class="hero-meta__item">
            <TrendingUp :size="15" />
            <span>已更新到当前统计口径</span>
          </div>
        </div>
      </div>
    </section>

    <div v-if="showCompensationHint" class="dashboard-hint">
      <AlertTriangle :size="16" />
      <span>当前有 {{ summary?.pendingCompensations }} 条待处理补偿记录，可能影响履约体验</span>
    </div>

    <div v-loading="loading" class="dashboard-body">
      <template v-if="dashboard && summary && trend && insights">
        <section class="summary-grid">
          <article class="summary-card">
            <div class="summary-card__head">
              <ChartColumn :size="18" />
              <span>营收</span>
            </div>
            <strong class="summary-card__metric">{{ formatCurrency(summary.revenue) }}</strong>
            <p
              class="summary-card__change"
              :data-tone="changeTone(summary.revenueChangeRate)"
              data-testid="summary-change"
            >
              较上周期 {{ formatChange(summary.revenueChangeRate) }}
            </p>
            <small>客单价 {{ formatCurrency(summary.avgOrderValue) }}</small>
          </article>

          <article class="summary-card">
            <div class="summary-card__head">
              <BarChart3 :size="18" />
              <span>订单</span>
            </div>
            <strong class="summary-card__metric">{{ summary.paidOrders }}</strong>
            <p
              class="summary-card__change"
              :data-tone="changeTone(summary.paidOrdersChangeRate)"
              data-testid="summary-change"
            >
              支付订单 {{ formatChange(summary.paidOrdersChangeRate) }}
            </p>
            <small>总订单 {{ summary.totalOrders }} · 支付转化 {{ formatPercent(summary.paidOrderRate) }}</small>
          </article>

          <article class="summary-card">
            <div class="summary-card__head">
              <Box :size="18" />
              <span>库存效率</span>
            </div>
            <strong class="summary-card__metric">{{ formatPercent(summary.inventoryConsumptionRate) }}</strong>
            <p
              class="summary-card__change"
              :data-tone="inventorySummaryTone(summary.inventoryConsumptionRate, summary.highConsumptionActivityCount)"
              data-testid="summary-change"
            >
              {{ inventorySummaryText }}
            </p>
            <div
              class="inventory-progress"
              data-testid="inventory-progress-summary"
              data-kind="inventory-progress"
              :data-tone="inventoryTone(summary.inventoryConsumptionRate)"
              role="progressbar"
              aria-label="库存消耗率"
              aria-valuemin="0"
              aria-valuemax="100"
              :aria-valuenow="formatProgressValue(summary.inventoryConsumptionRate)"
            >
              <div
                class="inventory-progress__fill"
                :style="{
                  width: `${formatProgressValue(summary.inventoryConsumptionRate)}%`,
                  background: inventoryProgressColor(inventoryTone(summary.inventoryConsumptionRate)),
                }"
              ></div>
            </div>
            <small>已消耗 {{ summary.inventoryConsumed }} / 总库存 {{ summary.inventoryTotal }}</small>
          </article>
        </section>

        <section class="flat-panel">
          <div class="panel-header">
            <TrendingUp :size="18" />
            <div>
              <h3>营收与支付订单趋势</h3>
              <p>{{ trend.periodLabel }}</p>
            </div>
          </div>
          <p class="panel-summary" data-testid="trend-summary">{{ trendSummary }}</p>
          <div v-if="trend.buckets.length > 0" ref="trendChartRef" class="chart-surface"></div>
          <div v-else class="empty-panel">当前周期暂无趋势数据</div>
        </section>

        <section class="flat-panel">
          <div class="panel-header">
            <BarChart3 :size="18" />
            <div>
              <h3>活动经营贡献</h3>
              <p>按当前周期营收排序，优先关注贡献和风险并存的活动</p>
            </div>
          </div>
          <div v-if="activityPerformance.length > 0" class="activity-table">
            <div class="activity-table__head">
              <span>活动</span>
              <span>阶段</span>
              <span>营收</span>
              <span>支付订单</span>
              <span>总订单</span>
              <span>支付转化</span>
              <span>库存消耗</span>
              <span>营收环比</span>
              <span>订单环比</span>
            </div>
            <button
              v-for="item in activityPerformance"
              :key="item.activityId"
              class="activity-table__row"
              type="button"
              @click="goActivity(item.activityId)"
            >
              <strong class="metric-primary">{{ item.title }}</strong>
              <span
                class="activity-phase"
                :data-phase="phaseTone(item.phase)"
                :data-testid="`activity-phase-${item.activityId}`"
              >
                {{ phaseLabel(item.phase) }}
              </span>
              <span class="metric-primary">{{ formatCurrency(item.revenue) }}</span>
              <span class="metric-primary">{{ item.paidOrders }}</span>
              <span class="metric-secondary">{{ item.totalOrders }}</span>
              <span class="metric-secondary">{{ formatPercent(item.paidOrderRate) }}</span>
              <div class="activity-inventory">
                <div
                  class="inventory-progress"
                  :data-testid="`inventory-progress-activity-${item.activityId}`"
                  data-kind="inventory-progress"
                  :data-tone="inventoryTone(item.inventoryConsumptionRate)"
                  role="progressbar"
                  :aria-label="`${item.title} 库存消耗率`"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  :aria-valuenow="formatProgressValue(item.inventoryConsumptionRate)"
                >
                  <div
                    class="inventory-progress__fill"
                    :style="{
                      width: `${formatProgressValue(item.inventoryConsumptionRate)}%`,
                      background: inventoryProgressColor(inventoryTone(item.inventoryConsumptionRate)),
                    }"
                  ></div>
                </div>
                <small>{{ formatPercent(item.inventoryConsumptionRate) }}</small>
              </div>
              <span class="metric-change" :data-tone="changeTone(item.revenueChangeRate)">
                {{ formatChange(item.revenueChangeRate) }}
              </span>
              <span class="metric-change metric-change--muted" :data-tone="changeTone(item.totalOrdersChangeRate)">
                {{ formatChange(item.totalOrdersChangeRate) }}
              </span>
            </button>
          </div>
          <div v-else class="empty-panel">当前周期暂无活动经营数据</div>
        </section>

        <section class="flat-grid flat-grid--2">
          <article class="flat-panel flat-panel--soft">
            <div class="panel-header">
              <Box :size="18" />
              <div>
                <h3>库存效率分层</h3>
                <p>关注供给利用节奏和转化承接状态</p>
              </div>
            </div>
            <div class="insight-stats">
              <div class="insight-stat" :data-tone="inventoryTone(0.9)">
                <span>高消耗</span>
                <strong>{{ insights.highConsumptionCount }}</strong>
                <small>{{ inventoryInsightCopy('high') }}</small>
              </div>
              <div class="insight-stat" :data-tone="inventoryTone(0.5)">
                <span>中消耗</span>
                <strong>{{ insights.mediumConsumptionCount }}</strong>
                <small>{{ inventoryInsightCopy('medium') }}</small>
              </div>
              <div class="insight-stat" :data-tone="inventoryTone(0.1)">
                <span>低消耗</span>
                <strong>{{ insights.lowConsumptionCount }}</strong>
                <small>{{ inventoryInsightCopy('low') }}</small>
              </div>
            </div>
          </article>

          <article class="flat-panel flat-panel--soft">
            <div class="panel-header">
              <AlertTriangle :size="18" />
              <div>
                <h3>经营提示</h3>
                <p>优先查看风险，再决定是否下钻排查</p>
              </div>
            </div>
            <ul class="insight-messages">
              <li
                v-for="message in insights.messages"
                :key="message"
                class="insight-message"
                :data-tone="messageTone(message)"
              >
                <strong>{{ message }}</strong>
                <span>结合当前活动趋势与库存状态进一步判断处理优先级。</span>
              </li>
            </ul>
          </article>
        </section>
      </template>

      <div v-else-if="!loading" class="empty-panel">当前周期暂无经营数据</div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-hero {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
  align-items: flex-start;
}

.dashboard-hero__main,
.dashboard-hero__controls {
  display: grid;
  gap: 0.75rem;
}

.dashboard-hero__controls {
  justify-items: end;
  min-width: 260px;
}

.dashboard-title {
  margin: 0;
  font-size: 2.5rem;
  line-height: 1;
}

.dashboard-copy {
  margin: 0;
  font-size: 1rem;
  color: var(--fg-soft);
}

.hero-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.hero-highlight {
  display: grid;
  gap: 0.15rem;
  min-width: 120px;
  padding: 0.7rem 0.85rem;
  border: 1px solid rgba(255, 255, 255, 0.24);
  background: rgb(8, 91, 255);
  color: rgba(255, 255, 255, 0.92);
}

.hero-highlight span {
  font-size: 0.8rem;
}

.hero-highlight strong {
  font-size: 1rem;
  line-height: 1.1;
}

.hero-highlight[data-tone='positive'] {
  background: rgb(16, 185, 129);
}

.hero-highlight[data-tone='warning'] {
  background: rgb(245, 159, 11);
}

.hero-highlight[data-tone='danger'] {
  background: rgb(239, 68, 68);
}

.dashboard-hint {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.8rem 1rem;
  border: 1px solid #f59e0b;
  background: #fffbeb;
  color: #92400e;
  font-weight: 600;
}

.hero-meta {
  display: grid;
  gap: 0.5rem;
  justify-items: end;
}

.hero-meta__item {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--fg-soft);
  font-size: 0.9rem;
}

.dashboard-body {
  display: grid;
  gap: 1rem;
}

.summary-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.summary-card {
  display: grid;
  gap: 0.65rem;
  padding: 1.25rem;
  border: 1px solid var(--border);
  background: white;
}

.summary-card__head {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--fg-soft);
  font-weight: 700;
}

.summary-card__metric {
  font-size: 2.3rem;
  line-height: 1;
}

.summary-card__change {
  margin: 0;
  font-size: 0.96rem;
  font-weight: 700;
}

.summary-card__change[data-tone='positive'],
.metric-change[data-tone='positive'] {
  color: #047857;
}

.summary-card__change[data-tone='danger'],
.metric-change[data-tone='danger'] {
  color: #b91c1c;
}

.summary-card__change[data-tone='warning'],
.metric-change[data-tone='warning'] {
  color: #b45309;
}

.summary-card__change[data-tone='neutral'],
.metric-change[data-tone='neutral'] {
  color: var(--fg-soft);
}

.summary-card small {
  color: var(--fg-soft);
}

.panel-header {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 0.85rem;
}

.panel-header h3,
.panel-header p,
.panel-summary {
  margin: 0;
}

.panel-header p {
  color: var(--fg-soft);
  margin-top: 0.25rem;
  font-size: 0.9rem;
}

.panel-summary {
  margin-bottom: 1rem;
  color: var(--fg);
  font-size: 0.94rem;
  font-weight: 600;
}

.chart-surface {
  width: 100%;
  height: 360px;
}

.activity-table {
  display: grid;
  border: 1px solid var(--border);
}

.activity-table__head,
.activity-table__row {
  display: grid;
  grid-template-columns: 1.8fr 0.9fr 1fr 0.9fr 0.9fr 0.9fr 1.2fr 0.9fr 0.9fr;
  gap: 0.75rem;
  align-items: center;
  padding: 0.9rem 1rem;
}

.activity-table__head {
  background: var(--muted);
  font-size: 0.8rem;
  font-weight: 800;
}

.activity-table__row {
  border: 0;
  border-top: 1px solid var(--border);
  background: white;
  text-align: left;
}

.activity-table__row:hover {
  background: #eff6ff;
}

.metric-primary {
  font-weight: 700;
  color: var(--fg);
}

.metric-secondary {
  color: var(--fg-soft);
}

.metric-change {
  font-weight: 700;
}

.metric-change--muted {
  font-size: 0.92rem;
}

.activity-phase {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 0.6rem;
  font-size: 0.8rem;
  font-weight: 700;
  white-space: nowrap;
}

.activity-phase[data-phase='preview'] {
  background: #dbeafe;
  color: #1d4ed8;
}

.activity-phase[data-phase='ongoing'] {
  background: #dcfce7;
  color: #047857;
}

.activity-phase[data-phase='ended'] {
  background: #e5e7eb;
  color: #4b5563;
}

.activity-phase[data-phase='draft'] {
  background: #fef3c7;
  color: #b45309;
}

.activity-phase[data-phase='offline'] {
  background: #fee2e2;
  color: #b91c1c;
}

.activity-inventory {
  display: grid;
  gap: 0.35rem;
}

.inventory-progress {
  --inventory-danger: #ef4444;
  --inventory-warning: #f59e0b;
  --inventory-info: #3b82f6;
  width: 100%;
  height: 10px;
  overflow: hidden;
  border: 1px solid var(--border);
  background: #e5e7eb;
}

.inventory-progress__fill {
  height: 100%;
  transition: width 0.2s ease;
}

.insight-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.insight-stat {
  display: grid;
  gap: 0.35rem;
  padding: 1rem;
  border: 1px solid var(--border);
  background: white;
}

.insight-stat span,
.insight-stat small {
  color: var(--fg-soft);
}

.insight-stat strong {
  font-size: 1.6rem;
}

.insight-stat[data-tone='danger'] {
  background: #fee2e2;
}

.insight-stat[data-tone='warning'] {
  background: #fef3c7;
}

.insight-stat[data-tone='info'] {
  background: #dbeafe;
}

.insight-messages {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 0.75rem;
}

.insight-message {
  display: grid;
  gap: 0.35rem;
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--border);
  background: white;
}

.insight-message strong {
  font-size: 0.95rem;
}

.insight-message span {
  color: var(--fg-soft);
  font-size: 0.88rem;
}

.insight-message[data-tone='positive'] {
  border-color: #86efac;
}

.insight-message[data-tone='warning'] {
  border-color: #fcd34d;
  background: #fffbeb;
}

.empty-panel {
  display: grid;
  place-items: center;
  min-height: 180px;
  border: 1px dashed var(--border);
  color: var(--fg-soft);
  background: #f9fafb;
}

@media (max-width: 1100px) {
  .summary-grid,
  .insight-stats {
    grid-template-columns: 1fr;
  }

  .dashboard-hero {
    flex-direction: column;
  }

  .dashboard-hero__controls,
  .hero-meta {
    justify-items: start;
  }

  .activity-table {
    overflow-x: auto;
  }

  .activity-table__head,
  .activity-table__row {
    min-width: 1080px;
  }
}
</style>
