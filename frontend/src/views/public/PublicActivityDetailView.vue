<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { publicActivityApi } from '@/api/public-activity'
import { seckillApi } from '@/api/seckill'
import { ApiClientError } from '@/api/request'
import StatusBadge from '@/components/StatusBadge.vue'
import { useAuthStore } from '@/stores/auth'
import { formatDisplayDateTime } from '@/utils/date'
import { getCodeSourceModeLabel, getPhaseLabel, getPublishStatusLabel } from '@/utils/activity'
import type { ActivityDetail, OrderCodeDetail, OrderDetail, PaymentOrder, SeckillResult } from '@/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const detail = ref<ActivityDetail | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const seckillAttempting = ref(false)
const resultRefreshing = ref(false)
const paymentCreating = ref(false)
const paymentCallbackSubmitting = ref(false)
const orderQuerying = ref(false)
const codeQuerying = ref(false)

const seckillResult = ref<SeckillResult | null>(null)
const paymentOrder = ref<PaymentOrder | null>(null)
const orderDetail = ref<OrderDetail | null>(null)
const orderCodeDetail = ref<OrderCodeDetail | null>(null)

let pollingTimer: ReturnType<typeof setTimeout> | null = null

const activityId = computed(() => Number(route.params.id))
const isAuthenticated = computed(() => authStore.isAuthenticated)
const currentOrderNo = computed(() => seckillResult.value?.orderNo ?? '')
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
    const latest = await refreshSeckillResult({ silent: true })
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

async function refreshSeckillResult(options: { silent?: boolean } = {}) {
  if (!detail.value || !isAuthenticated.value) {
    seckillResult.value = null
    return null
  }

  if (!options.silent) {
    resultRefreshing.value = true
  }

  try {
    const latest = await seckillApi.queryResult(activityId.value)
    seckillResult.value = latest
    if (!latest.orderNo) {
      paymentOrder.value = null
      orderDetail.value = null
      orderCodeDetail.value = null
    }
    return latest
  } catch (error) {
    if (!options.silent) {
      const message = error instanceof ApiClientError ? error.message : '抢购结果查询失败'
      ElMessage.error(message)
    }
    return null
  } finally {
    if (!options.silent) {
      resultRefreshing.value = false
    }
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
      const latest = await refreshSeckillResult({ silent: true })
      if (shouldPoll(latest?.status)) {
        schedulePolling()
      }
      return
    }

    stopPolling()
    ElMessage.warning(response.message || '抢购请求未通过')
    await refreshSeckillResult({ silent: true })
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '抢购请求失败'
    ElMessage.error(message)
  } finally {
    seckillAttempting.value = false
  }
}

async function handleRefreshResult() {
  const latest = await refreshSeckillResult()
  if (shouldPoll(latest?.status)) {
    schedulePolling()
    return
  }
  stopPolling()
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
    const latest = await refreshSeckillResult({ silent: true })
    if (shouldPoll(latest?.status)) {
      schedulePolling()
      return
    }
    stopPolling()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '模拟支付回调失败'
    ElMessage.error(message)
  } finally {
    paymentCallbackSubmitting.value = false
  }
}

async function handleQueryOrder() {
  if (!currentOrderNo.value) {
    ElMessage.error('当前没有订单号')
    return
  }

  orderQuerying.value = true
  try {
    orderDetail.value = await seckillApi.queryOrder(currentOrderNo.value)
    ElMessage.success('订单详情已刷新')
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '订单详情查询失败'
    ElMessage.error(message)
  } finally {
    orderQuerying.value = false
  }
}

async function handleQueryCode() {
  if (!currentOrderNo.value) {
    ElMessage.error('当前没有订单号')
    return
  }

  codeQuerying.value = true
  try {
    orderCodeDetail.value = await seckillApi.queryOrderCode(currentOrderNo.value)
    ElMessage.success('兑换码信息已刷新')
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '兑换码查询失败'
    ElMessage.error(message)
  } finally {
    codeQuerying.value = false
  }
}

watch(
  () => isAuthenticated.value,
  async (authenticated) => {
    if (!authenticated) {
      stopPolling()
      seckillResult.value = null
      paymentOrder.value = null
      orderDetail.value = null
      orderCodeDetail.value = null
      return
    }

    const latest = await refreshSeckillResult({ silent: true })
    if (shouldPoll(latest?.status)) {
      schedulePolling()
    }
  },
)

watch(
  () => currentOrderNo.value,
  () => {
    paymentOrder.value = null
    orderDetail.value = null
    orderCodeDetail.value = null
  },
)

onMounted(async () => {
  await loadDetail()
  const latest = await refreshSeckillResult({ silent: true })
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

    <section class="flat-panel flat-panel--green seckill-panel">
      <div class="seckill-panel__header">
        <div>
          <div class="eyebrow">Seckill</div>
          <h2 class="seckill-panel__title">抢购操作</h2>
        </div>
        <div class="seckill-panel__badges">
          <StatusBadge :label="isAuthenticated ? '已登录' : '未登录'" :tone="isAuthenticated ? 'green' : 'slate'" />
          <StatusBadge :label="canAttemptSeckill ? '可抢购' : '暂不可抢购'" :tone="canAttemptSeckill ? 'blue' : 'amber'" />
        </div>
      </div>

      <div class="seckill-panel__actions">
        <button class="flat-button" type="button" :disabled="!canAttemptSeckill" @click="handleAttemptSeckill">
          {{ seckillAttempting ? '提交中...' : '立即抢购' }}
        </button>
        <button class="flat-button flat-button--ghost" type="button" :disabled="!isAuthenticated || resultRefreshing" @click="handleRefreshResult">
          {{ resultRefreshing ? '刷新中...' : '刷新抢购结果' }}
        </button>
      </div>

      <p class="seckill-panel__tip" v-if="attemptBlockedReason">
        {{ attemptBlockedReason }}
      </p>

      <article class="flat-panel seckill-result" v-if="seckillResult">
        <div class="seckill-result__header">
          <div>
            <div class="eyebrow">Result</div>
            <strong>{{ getSeckillStatusLabel(seckillResult.status) }}</strong>
          </div>
          <StatusBadge :label="getSeckillStatusLabel(seckillResult.status)" :tone="getSeckillStatusTone(seckillResult.status)" />
        </div>
        <div class="meta-list">
          <div class="meta-row"><span>状态说明</span><strong>{{ seckillResult.message || '-' }}</strong></div>
          <div class="meta-row"><span>订单号</span><strong>{{ seckillResult.orderNo || '-' }}</strong></div>
          <div class="meta-row"><span>结果更新时间</span><strong>{{ seckillResult.updatedAt ? formatDisplayDateTime(seckillResult.updatedAt) : '-' }}</strong></div>
          <div class="meta-row"><span>兑换码</span><strong>{{ seckillResult.code || '-' }}</strong></div>
        </div>
      </article>

      <article class="flat-panel flat-panel--soft payment-panel" v-if="showPaymentPanel">
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
      </article>

      <article class="flat-panel order-panel" v-if="currentOrderNo">
        <div class="order-panel__header">
          <div>
            <div class="eyebrow">Order</div>
            <h3 class="order-panel__title">订单与兑换码查询</h3>
          </div>
          <div class="order-panel__actions">
            <button class="flat-button flat-button--ghost" type="button" :disabled="orderQuerying" @click="handleQueryOrder">
              {{ orderQuerying ? '查询中...' : '刷新订单详情' }}
            </button>
            <button class="flat-button flat-button--ghost" type="button" :disabled="codeQuerying" @click="handleQueryCode">
              {{ codeQuerying ? '查询中...' : '刷新兑换码信息' }}
            </button>
          </div>
        </div>

        <div class="order-panel__empty" v-if="!orderDetail && !orderCodeDetail">
          可按需查询订单详情与兑换码信息。
        </div>

        <div class="flat-grid flat-grid--2" v-else>
          <article class="flat-panel flat-panel--soft" v-if="orderDetail">
            <div class="eyebrow">Order Detail</div>
            <div class="meta-list">
              <div class="meta-row"><span>订单号</span><strong>{{ orderDetail.orderNo }}</strong></div>
              <div class="meta-row"><span>订单状态</span><strong>{{ orderDetail.orderStatus }}</strong></div>
              <div class="meta-row"><span>支付状态</span><strong>{{ orderDetail.payStatus }}</strong></div>
              <div class="meta-row"><span>兑换码状态</span><strong>{{ orderDetail.codeStatus }}</strong></div>
              <div class="meta-row"><span>订单金额</span><strong>{{ orderDetail.priceAmount }}</strong></div>
              <div class="meta-row"><span>失败原因</span><strong>{{ orderDetail.failReason || '-' }}</strong></div>
            </div>
          </article>

          <article class="flat-panel flat-panel--soft" v-if="orderCodeDetail">
            <div class="eyebrow">Redeem Code</div>
            <div class="meta-list">
              <div class="meta-row"><span>订单号</span><strong>{{ orderCodeDetail.orderNo }}</strong></div>
              <div class="meta-row"><span>订单状态</span><strong>{{ orderCodeDetail.orderStatus }}</strong></div>
              <div class="meta-row"><span>支付状态</span><strong>{{ orderCodeDetail.payStatus }}</strong></div>
              <div class="meta-row"><span>发码状态</span><strong>{{ orderCodeDetail.codeStatus }}</strong></div>
              <div class="meta-row"><span>兑换码</span><strong>{{ orderCodeDetail.code || '-' }}</strong></div>
            </div>
          </article>
        </div>
      </article>
    </section>
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
.seckill-panel__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.seckill-panel {
  display: grid;
  gap: 1rem;
}

.seckill-panel__header,
.seckill-result__header,
.payment-panel__header,
.order-panel__header,
.order-panel__actions,
.payment-panel__actions,
.seckill-panel__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.seckill-panel__title,
.payment-panel__title,
.order-panel__title {
  margin: 0.25rem 0 0;
  font-size: 1.35rem;
}

.seckill-panel__tip,
.order-panel__empty {
  color: var(--fg-soft);
}

.order-panel__empty {
  padding: 0.95rem 1rem;
  border: 2px dashed rgba(17, 24, 39, 0.4);
  background: rgba(255, 255, 255, 0.65);
}

@media (max-width: 960px) {
  .detail-hero,
  .detail-grid,
  .seckill-panel__header,
  .seckill-result__header,
  .payment-panel__header,
  .order-panel__header,
  .order-panel__actions,
  .payment-panel__actions,
  .seckill-panel__actions {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
