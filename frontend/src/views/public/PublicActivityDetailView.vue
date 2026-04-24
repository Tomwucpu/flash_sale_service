<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshCw } from 'lucide-vue-next'
import { publicActivityApi } from '@/api/public-activity'
import { seckillApi } from '@/api/seckill'
import { ApiClientError } from '@/api/request'
import StatusBadge from '@/components/StatusBadge.vue'
import { useAuthStore } from '@/stores/auth'
import { formatDisplayDateTime } from '@/utils/date'
import { getCodeSourceModeLabel, getPhaseLabel, getPublishStatusLabel } from '@/utils/activity'
import {
  codeStatusTone,
  formatOrderAmount,
  getCodeStatusLabel,
  getOrderStatusLabel,
  getPayStatusLabel,
  orderStatusTone,
  payStatusTone,
  summarizeActivityOrders,
} from '@/utils/order'
import type { ActivityDetail, OrderDetail, PaymentOrder, SeckillResult } from '@/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const detail = ref<ActivityDetail | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const seckillAttempting = ref(false)
const paymentCreating = ref(false)
const paymentCallbackSubmitting = ref(false)
const orderQuerying = ref(false)

const seckillResult = ref<SeckillResult | null>(null)
const paymentOrder = ref<PaymentOrder | null>(null)
const activityOrders = ref<OrderDetail[]>([])

let pollingTimer: ReturnType<typeof setTimeout> | null = null

const activityId = computed(() => Number(route.params.id))
const isAuthenticated = computed(() => authStore.isAuthenticated)
const currentOrderNo = computed(() => seckillResult.value?.orderNo ?? '')
const seckillResultRows = computed(() => (seckillResult.value ? [seckillResult.value] : []))
const hasActivityOrders = computed(() => activityOrders.value.length > 0)
const activityOrderSummary = computed(() => summarizeActivityOrders(activityOrders.value))
const showPaymentPanel = computed(
  () =>
    Boolean(detail.value?.needPayment) &&
    seckillResult.value?.status === 'PENDING_PAYMENT' &&
    Boolean(currentOrderNo.value),
)

const canAttemptSeckill = computed(() => {
  if (!detail.value || !isAuthenticated.value || seckillAttempting.value) {
    return false
  }
  return detail.value.publishStatus === 'PUBLISHED' && detail.value.phase === 'ONGOING'
})

const attemptBlockedReason = computed(() => {
  if (!detail.value) {
    return '活动详情尚未加载完成'
  }
  if (!isAuthenticated.value) {
    return '请先登录后再参与抢购'
  }
  if (detail.value.publishStatus !== 'PUBLISHED') {
    return '当前活动暂不可抢购'
  }
  if (detail.value.phase === 'PREVIEW') {
    return '活动未开始'
  }
  if (detail.value.phase === 'ENDED') {
    return '活动已结束'
  }
  return ''
})

function getSeckillStatusLabel(status: string) {
  const map: Record<string, string> = {
    INIT: '未抢购',
    PROCESSING: '处理中',
    PENDING_PAYMENT: '待支付',
    SUCCESS: '抢购成功',
    FAIL: '抢购失败',
  }
  return map[status] ?? status
}

function getSeckillStatusTone(status: string): 'blue' | 'green' | 'amber' | 'slate' {
  if (status === 'SUCCESS') {
    return 'green'
  }
  if (status === 'PROCESSING') {
    return 'blue'
  }
  if (status === 'PENDING_PAYMENT') {
    return 'amber'
  }
  return 'slate'
}

function shouldPoll(status: string | undefined) {
  return status === 'PROCESSING' || status === 'PENDING_PAYMENT'
}

function stopPolling() {
  if (pollingTimer) {
    clearTimeout(pollingTimer)
    pollingTimer = null
  }
}

function schedulePolling() {
  stopPolling()
  pollingTimer = setTimeout(async () => {
    const latest = await refreshSeckillResult()
    if (shouldPoll(latest?.status)) {
      schedulePolling()
      return
    }
    stopPolling()
  }, 2500)
}

async function loadDetail() {
  loading.value = true
  errorMessage.value = ''
  try {
    detail.value = await publicActivityApi.detail(activityId.value)
  } catch (error) {
    detail.value = null
    errorMessage.value = error instanceof ApiClientError ? error.message : '活动详情加载失败'
  } finally {
    loading.value = false
  }
}

async function refreshSeckillResult() {
  if (!detail.value || !isAuthenticated.value) {
    seckillResult.value = null
    return null
  }

  try {
    const latest = await seckillApi.queryResult(activityId.value)
    seckillResult.value = latest
    if (!latest.orderNo) {
      paymentOrder.value = null
    }
    return latest
  } catch {
    return null
  }
}

async function handleAttemptSeckill() {
  if (!detail.value) {
    return
  }

  if (!isAuthenticated.value) {
    ElMessage.warning('请先登录后再参与抢购')
    await router.push({
      path: '/login',
      query: {
        redirect: `/public/activities/${activityId.value}`,
      },
    })
    return
  }

  if (!canAttemptSeckill.value) {
    ElMessage.warning(attemptBlockedReason.value || '当前不可抢购')
    return
  }

  seckillAttempting.value = true
  try {
    const response = await seckillApi.attempt(activityId.value)
    if (response.code === 'SECKILL_PROCESSING') {
      ElMessage.success(response.message)
      const latest = await refreshSeckillResult()
      if (shouldPoll(latest?.status)) {
        schedulePolling()
      }
      return
    }

    stopPolling()
    ElMessage.warning(response.message || '抢购请求未通过')
    await refreshSeckillResult()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '抢购请求失败'
    ElMessage.error(message)
  } finally {
    seckillAttempting.value = false
  }
}

async function handleCreatePayment() {
  if (!currentOrderNo.value) {
    ElMessage.error('当前没有可支付订单')
    return
  }

  paymentCreating.value = true
  try {
    paymentOrder.value = await seckillApi.createPayment(currentOrderNo.value)
    ElMessage.success('模拟支付单已创建')
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '创建模拟支付单失败'
    ElMessage.error(message)
  } finally {
    paymentCreating.value = false
  }
}

async function handlePaymentCallback() {
  if (!currentOrderNo.value) {
    ElMessage.error('当前没有可支付订单')
    return
  }

  if (!paymentOrder.value?.transactionNo) {
    ElMessage.warning('请先创建模拟支付单')
    return
  }

  paymentCallbackSubmitting.value = true
  try {
    paymentOrder.value = await seckillApi.callbackPayment({
      orderNo: currentOrderNo.value,
      transactionNo: paymentOrder.value.transactionNo,
    })
    ElMessage.success('模拟支付回调成功，正在刷新结果')
    const latest = await refreshSeckillResult()
    if (shouldPoll(latest?.status)) {
      schedulePolling()
      return
    }
    await handleQueryOrders({ silent: true })
    stopPolling()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '模拟支付回调失败'
    ElMessage.error(message)
  } finally {
    paymentCallbackSubmitting.value = false
  }
}

async function handleQueryOrders(options: { silent?: boolean } = {}) {
  if (!isAuthenticated.value) {
    activityOrders.value = []
    if (!options.silent) {
      ElMessage.warning('请先登录后查看订单')
    }
    return
  }
  orderQuerying.value = true
  try {
    activityOrders.value = await seckillApi.queryActivityOrders(activityId.value)
    if (!options.silent) {
      ElMessage.success('订单与兑换码已刷新')
    }
  } catch (error) {
    activityOrders.value = []
    if (!options.silent) {
      const message = error instanceof ApiClientError ? error.message : '订单与兑换码查询失败'
      ElMessage.error(message)
    }
  }
  orderQuerying.value = false
}

watch(
  () => isAuthenticated.value,
  async (authenticated) => {
    if (!authenticated) {
      stopPolling()
      seckillResult.value = null
      paymentOrder.value = null
      activityOrders.value = []
      return
    }

    const latest = await refreshSeckillResult()
    await handleQueryOrders({ silent: true })
    if (shouldPoll(latest?.status)) {
      schedulePolling()
    }
  },
)

watch(
  () => currentOrderNo.value,
  () => {
    paymentOrder.value = null
  },
)

onMounted(async () => {
  await loadDetail()
  const latest = await refreshSeckillResult()
  if (isAuthenticated.value) {
    await handleQueryOrders({ silent: true })
  }
  if (shouldPoll(latest?.status)) {
    schedulePolling()
  }
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
  <div class="page-shell" v-loading="loading" v-if="detail">
    <section class="detail-hero">
      <div class="detail-hero__image">
        <img :src="detail.coverUrl" :alt="detail.title" />
      </div>
      <div class="detail-hero__content">
        <div class="eyebrow">Public Detail</div>
        <h1 class="poster-title">{{ detail.title }}</h1>
        <p class="poster-copy">{{ detail.description }}</p>
        <div class="detail-hero__badges">
          <StatusBadge
            :label="getPublishStatusLabel(detail.publishStatus)"
            :tone="detail.publishStatus === 'PUBLISHED' ? 'green' : detail.publishStatus === 'UNPUBLISHED' ? 'amber' : 'slate'"
          />
          <StatusBadge
            :label="getPhaseLabel(detail.phase)"
            :tone="detail.phase === 'ONGOING' ? 'blue' : detail.phase === 'PREVIEW' ? 'amber' : 'slate'"
          />
        </div>
        <div class="detail-hero__seckill">
          <button class="flat-button detail-hero__seckill-button" type="button" :disabled="!canAttemptSeckill" @click="handleAttemptSeckill">
            {{ seckillAttempting ? '提交中...' : '立即抢购' }}
          </button>

          <div class="detail-hero__payment" v-if="showPaymentPanel">
            <div class="payment-panel__header">
              <div>
                <div class="eyebrow">Payment</div>
                <h3 class="payment-panel__title">待支付订单处理</h3>
              </div>
              <StatusBadge label="待支付" tone="amber" />
            </div>
            <div class="payment-panel__actions">
              <button class="flat-button flat-button--secondary" type="button" :disabled="paymentCreating" @click="handleCreatePayment">
                {{ paymentCreating ? '创建中...' : '创建模拟支付单' }}
              </button>
              <button class="flat-button" type="button" :disabled="paymentCallbackSubmitting || !paymentOrder?.transactionNo" @click="handlePaymentCallback">
                {{ paymentCallbackSubmitting ? '回调中...' : '提交模拟支付回调' }}
              </button>
            </div>
            <div class="meta-list" v-if="paymentOrder">
              <div class="meta-row"><span>支付单订单号</span><strong>{{ paymentOrder.orderNo }}</strong></div>
              <div class="meta-row"><span>交易流水号</span><strong>{{ paymentOrder.transactionNo }}</strong></div>
              <div class="meta-row"><span>支付金额</span><strong>{{ paymentOrder.payAmount }}</strong></div>
              <div class="meta-row"><span>支付状态</span><strong>{{ paymentOrder.payStatus }}</strong></div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="detail-grid">
      <article class="flat-panel">
        <div class="eyebrow">Offer</div>
        <div class="meta-list">
          <div class="meta-row"><span>价格</span><strong>{{ detail.priceAmount }}</strong></div>
          <div class="meta-row"><span>库存</span><strong>{{ detail.availableStock }} / {{ detail.totalStock }}</strong></div>
          <div class="meta-row"><span>支付要求</span><strong>{{ detail.needPayment ? '需要支付' : '免支付' }}</strong></div>
          <div class="meta-row"><span>兑换码来源</span><strong>{{ getCodeSourceModeLabel(detail.codeSourceMode) }}</strong></div>
        </div>
      </article>
      <article class="flat-panel flat-panel--soft">
        <div class="eyebrow">Timing</div>
        <div class="meta-list">
          <div class="meta-row"><span>发布时间</span><strong>{{ formatDisplayDateTime(detail.publishTime) }}</strong></div>
          <div class="meta-row"><span>开始时间</span><strong>{{ formatDisplayDateTime(detail.startTime) }}</strong></div>
          <div class="meta-row"><span>结束时间</span><strong>{{ formatDisplayDateTime(detail.endTime) }}</strong></div>
          <div class="meta-row"><span>限购规则</span><strong>{{ detail.purchaseLimitType === 'SINGLE' ? '单人一次' : '单人多次' }} / {{ detail.purchaseLimitCount }}</strong></div>
        </div>
      </article>
    </section>

      <article class="flat-panel seckill-result" v-if="seckillResult">
        <div class="seckill-result__header">
          <div>
            <div class="eyebrow">Result</div>
            <h3 class="seckill-result__title">抢购结果</h3>
          </div>
        </div>
        <div class="seckill-result-table-wrap">
          <el-table class="seckill-result-table" :data="seckillResultRows" row-key="status">
            <el-table-column label="状态" width="112">
              <template #default="{ row }">
                <StatusBadge :label="getSeckillStatusLabel(row.status)" :tone="getSeckillStatusTone(row.status)" />
              </template>
            </el-table-column>
            <el-table-column label="状态说明" min-width="170" show-overflow-tooltip>
              <template #default="{ row }">
                <strong class="order-inline-cell">{{ row.message || '-' }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="订单号" min-width="210" show-overflow-tooltip>
              <template #default="{ row }">
                <strong class="order-no">{{ row.orderNo || '-' }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="兑换码" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <strong class="code-value">{{ row.code || '-' }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="170">
              <template #default="{ row }">
                <span class="order-inline-cell">{{ row.updatedAt ? formatDisplayDateTime(row.updatedAt) : '-' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </article>

      <article class="flat-panel order-panel" v-if="isAuthenticated">
        <div class="order-panel__header">
          <div class="order-panel__heading">
            <div class="eyebrow">Order</div>
            <h3 class="order-panel__title">订单与兑换码列表</h3>
            <div class="order-panel__summary" v-if="hasActivityOrders">
              <span>共 {{ activityOrderSummary.total }} 单</span>
              <span>{{ activityOrderSummary.issuedCodes }} 个已发码</span>
              <span>{{ activityOrderSummary.waitingPayment }} 单待支付</span>
            </div>
          </div>
          <div class="order-panel__actions">
            <button class="flat-button flat-button--ghost" type="button" :disabled="orderQuerying" @click="handleQueryOrders()">
              <RefreshCw :size="18" />
              {{ orderQuerying ? '查询中...' : '刷新' }}
            </button>
          </div>
        </div>

        <div v-loading="orderQuerying" class="order-table-loading">
          <div class="order-panel__empty" v-if="!hasActivityOrders">
            当前活动暂无订单记录。
          </div>

          <div class="order-table-wrap" v-else>
            <el-table class="order-table" :data="activityOrders" row-key="orderNo">
              <el-table-column prop="orderNo" label="订单号" min-width="210" show-overflow-tooltip>
                <template #default="{ row }">
                  <strong class="order-no">{{ row.orderNo }}</strong>
                </template>
              </el-table-column>
              <el-table-column label="金额" width="110">
                <template #default="{ row }">
                  <strong class="order-inline-cell">{{ formatOrderAmount(row.priceAmount) }}</strong>
                </template>
              </el-table-column>
              <el-table-column label="订单" width="112">
                <template #default="{ row }">
                  <StatusBadge :label="getOrderStatusLabel(row.orderStatus)" :tone="orderStatusTone(row.orderStatus)" />
                </template>
              </el-table-column>
              <el-table-column label="支付" width="112">
                <template #default="{ row }">
                  <StatusBadge :label="getPayStatusLabel(row.payStatus)" :tone="payStatusTone(row.payStatus)" />
                </template>
              </el-table-column>
              <el-table-column label="发码" width="112">
                <template #default="{ row }">
                  <StatusBadge :label="getCodeStatusLabel(row.codeStatus)" :tone="codeStatusTone(row.codeStatus)" />
                </template>
              </el-table-column>
              <el-table-column label="兑换码" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">
                  <strong class="code-value">{{ row.code || '-' }}</strong>
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="150" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="muted-line">{{ row.failReason || '无异常' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="更新时间" min-width="170">
                <template #default="{ row }">
                  <span class="order-inline-cell">{{ formatDisplayDateTime(row.updatedAt) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </article>
  </div>
  <div class="page-shell" v-else>
    <section class="flat-panel flat-panel--amber">
      <div class="eyebrow">Public Detail</div>
      <h1 class="poster-title">活动不存在</h1>
      <p class="poster-copy">{{ errorMessage || '你访问的活动不存在。' }}</p>
    </section>
  </div>
</template>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 1rem;
}

.detail-hero__image,
.detail-hero__content {
  border: 2px solid var(--fg);
}

.detail-hero__image img {
  width: 100%;
  height: 100%;
  min-height: 420px;
  object-fit: cover;
  display: block;
}

.detail-hero__content {
  display: grid;
  align-content: center;
  gap: 1rem;
  padding: 2rem;
  background: #dbeafe;
}

.detail-hero__badges,
.detail-hero__seckill-status {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.detail-hero__seckill {
  display: grid;
  gap: 0.75rem;
  padding-top: 1rem;
}

.detail-hero__seckill-button {
  justify-self: start;
  min-width: 160px;
}

.detail-hero__payment {
  display: grid;
  gap: 0.75rem;
  margin-top: 0.25rem;
  padding: 1rem;
  border: 2px solid rgba(17, 24, 39, 0.18);
  background: rgba(255, 255, 255, 0.5);
}

.seckill-result__header,
.payment-panel__header,
.order-panel__header,
.order-panel__actions,
.payment-panel__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.seckill-result__title,
.payment-panel__title,
.order-panel__title {
  margin: 0.25rem 0 0;
  font-size: 1.35rem;
}

.seckill-result,
.seckill-result-table-wrap {
  min-width: 0;
}

.seckill-result {
  display: grid;
  gap: 1rem;
  overflow: hidden;
}

.seckill-result-table-wrap {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}

.seckill-result-table {
  width: 100%;
}

.seckill-result-table :deep(.el-table__cell) {
  vertical-align: middle;
}

.order-panel__empty {
  color: var(--fg-soft);
}

.order-panel,
.order-table-loading,
.order-table-wrap {
  min-width: 0;
}

.order-panel {
  display: grid;
  gap: 1rem;
  overflow: hidden;
}

.order-panel__heading {
  min-width: 0;
}

.order-panel__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.6rem;
}

.order-panel__summary span {
  border: 1px solid rgba(17, 24, 39, 0.16);
  background: rgba(255, 255, 255, 0.7);
  color: var(--fg-soft);
  font-size: 0.86rem;
  padding: 0.24rem 0.55rem;
  white-space: nowrap;
}

.order-panel__empty {
  display: grid;
  min-height: 128px;
  place-items: center;
  padding: 0.95rem 1rem;
  border: 2px dashed rgba(17, 24, 39, 0.4);
  background: rgba(255, 255, 255, 0.65);
}

.order-table-wrap {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}

.order-table {
  width: 100%;
}

.order-table :deep(.el-table__cell) {
  vertical-align: middle;
}

.order-inline-cell,
.order-no,
.code-value,
.muted-line {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted-line {
  color: var(--fg-soft);
}

@media (max-width: 960px) {
  .detail-hero,
  .detail-grid,
  .seckill-result__header,
  .payment-panel__header,
  .order-panel__header,
  .order-panel__actions,
  .payment-panel__actions {
    grid-template-columns: 1fr;
    display: grid;
  }

  .detail-hero__seckill-button {
    justify-self: stretch;
  }
}
</style>
