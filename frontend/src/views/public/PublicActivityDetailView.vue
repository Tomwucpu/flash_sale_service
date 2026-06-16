<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from 'lucide-vue-next'
import { publicActivityApi } from '@/api/public-activity'
import { seckillApi } from '@/api/seckill'
import { ApiClientError } from '@/api/request'
import ActivityStatusBadges from '@/components/ActivityStatusBadges.vue'
import SeckillPanel from '@/components/PublicActivityDetail/SeckillPanel.vue'
import SeckillResultTable from '@/components/PublicActivityDetail/SeckillResultTable.vue'
import OrderPanel from '@/components/PublicActivityDetail/OrderPanel.vue'
import { useAuthStore } from '@/stores/auth'
import { formatDisplayDateTime } from '@/utils/date'
import { getCodeSourceModeLabel } from '@/utils/activity'
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
const showPaymentPanel = computed(
  () =>
    Boolean(detail.value?.needPayment) &&
    seckillResult.value?.status === 'PENDING_PAYMENT' &&
    Boolean(currentOrderNo.value),
)
const isOutOfStock = computed(() => {
  const activity = detail.value
  return activity !== null && activity.availableStock <= 0
})
const seckillButtonLabel = computed(() => {
  if (isOutOfStock.value) {
    return '暂无库存'
  }
  return seckillAttempting.value ? '提交中...' : '立即抢购'
})

const canAttemptSeckill = computed(() => {
  if (!detail.value || !isAuthenticated.value || seckillAttempting.value || isOutOfStock.value) {
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
  if (isOutOfStock.value) {
    return '暂无库存'
  }
  return ''
})

function shouldPoll(status: string | undefined) {
  return status === 'PROCESSING' || status === 'PENDING_PAYMENT'
}

function stopPolling() {
  if (pollingTimer) {
    clearTimeout(pollingTimer)
    pollingTimer = null
  }
}

function goToPublicActivities() {
  router.push('/public/activities')
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
    <div class="detail-hero__top">
      <button
        class="flat-button flat-button--ghost detail-hero__back"
        type="button"
        data-testid="back-to-public-activities"
        @click="goToPublicActivities"
      >
        <ArrowLeft :size="18" />
        返回活动列表
      </button>
    </div>
    <section class="detail-hero">
      <div class="detail-hero__image">
        <img :src="detail.coverUrl" :alt="detail.title" />
      </div>
      <div class="detail-hero__content">
        <div class="eyebrow">Public Detail</div>
        <h1 class="poster-title">{{ detail.title }}</h1>
        <p class="poster-copy">{{ detail.description }}</p>
        <ActivityStatusBadges :publish-status="detail.publishStatus" :phase="detail.phase" />
        <SeckillPanel
          v-if="detail.phase === 'ONGOING'"
          :can-attempt="canAttemptSeckill"
          :button-label="seckillButtonLabel"
          :attempting="seckillAttempting"
          :show-payment="showPaymentPanel"
          :payment-order="paymentOrder"
          :payment-creating="paymentCreating"
          :payment-callback-submitting="paymentCallbackSubmitting"
          @attempt="handleAttemptSeckill"
          @create-payment="handleCreatePayment"
          @submit-callback="handlePaymentCallback"
        />
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

    <SeckillResultTable :seckill-result="seckillResult" />

    <OrderPanel
      :activity-orders="activityOrders"
      :order-querying="orderQuerying"
      :is-authenticated="isAuthenticated"
      @query-orders="handleQueryOrders()"
    />
  </div>
  <div class="page-shell" v-else>
    <button
      class="flat-button flat-button--ghost detail-hero__back"
      type="button"
      data-testid="back-to-public-activities-empty"
      @click="goToPublicActivities"
    >
      <ArrowLeft :size="18" />
      返回活动列表
    </button>
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
  min-height: 280px;
  max-height: 360px;
  object-fit: cover;
  display: block;
}

.detail-hero__content {
  display: grid;
  align-content: center;
  gap: 0.75rem;
  padding: 1.5rem;
  background: #dbeafe;
}

.detail-hero__top {
  display: flex;
  justify-content: flex-start;
}

.detail-hero__back {
  min-height: 44px;
  padding: 0.65rem 0.9rem;
}

@media (max-width: 960px) {
  .detail-hero,
  .detail-grid {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
