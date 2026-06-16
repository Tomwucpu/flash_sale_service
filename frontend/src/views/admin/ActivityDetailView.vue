<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Megaphone, PenSquare, ReceiptText, SquareArrowOutUpRight } from 'lucide-vue-next'
import { activityApi } from '@/api/activity'
import { ApiClientError } from '@/api/request'
import ActivityStatusBadges from '@/components/ActivityStatusBadges.vue'
import ImportBatchPanel from '@/components/ActivityDetail/ImportBatchPanel.vue'
import ImportBatchDialog from '@/components/ActivityDetail/ImportBatchDialog.vue'
import {
  getCodeSourceModeLabel,
  getPublishModeLabel,
  isEditableActivity,
  shouldShowCodeImportPanel,
} from '@/utils/activity'
import { formatDisplayDateTime } from '@/utils/date'
import type { ActivityDetail } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<ActivityDetail | null>(null)
const batchDialogVisible = ref(false)
const selectedBatchNo = ref<string | null>(null)

const activityId = computed(() => Number(route.params.id))
const isAdvancePublish = computed(() => detail.value?.publishMode === 'SCHEDULED')
const publishActionLabel = computed(() => (isAdvancePublish.value ? '提前发布活动' : '立即发布活动'))

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await activityApi.detail(activityId.value)
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '活动详情加载失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

async function handlePublish() {
  await ElMessageBox.confirm(
    isAdvancePublish.value ? '确认提前发布该定时活动？' : '确认立即发布当前活动？',
    publishActionLabel.value,
    {
    type: 'warning',
    },
  )
  if (isAdvancePublish.value) {
    await activityApi.advancePublish(activityId.value)
  } else {
    await activityApi.publish(activityId.value)
  }
  ElMessage.success(isAdvancePublish.value ? '活动已提前发布' : '活动已立即发布')
  await loadDetail()
}

async function handleOffline() {
  await ElMessageBox.confirm('确认执行下线动作？', '下线活动', {
    type: 'warning',
  })
  await activityApi.offline(activityId.value)
  ElMessage.success('活动已下线')
  await loadDetail()
}

function handleViewBatchDetail(batchNo: string) {
  selectedBatchNo.value = batchNo
  batchDialogVisible.value = true
}

onMounted(loadDetail)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="page-header page-header--amber" v-if="detail">
      <div class="eyebrow">Activity Detail</div>
      <h1 class="poster-title">{{ detail.title }}</h1>
      <p class="poster-copy">{{ detail.description || '当前活动没有填写补充描述。' }}</p>
      <ActivityStatusBadges :publish-status="detail.publishStatus" :phase="detail.phase" />
    </section>

    <section class="detail-grid" v-if="detail">
      <article class="flat-panel">
        <div class="eyebrow">Overview</div>
        <div class="meta-list">
          <div class="meta-row"><span>活动 ID</span><strong>#{{ detail.id }}</strong></div>
          <div class="meta-row"><span>封面图</span><strong>{{ detail.coverUrl || '未设置' }}</strong></div>
          <div class="meta-row"><span>可用 / 总库存</span><strong><span style="color: #2563eb">{{ detail.availableStock }}</span> / {{ detail.totalStock }}</strong></div>
          <div class="meta-row" v-if="detail.codeSourceMode === 'THIRD_PARTY_IMPORTED'"><span>当前总有效兑换码</span><strong style="color: #16a34a">{{ detail.currentTotalImportedCount ?? 0 }}</strong></div>
          <div class="meta-row"><span>活动金额</span><strong>{{ detail.priceAmount }}</strong></div>
          <div class="meta-row"><span>支付模式</span><strong>{{ detail.needPayment ? '需要支付' : '免支付' }}</strong></div>
        </div>
      </article>

      <article class="flat-panel flat-panel--soft">
        <div class="eyebrow">Schedule</div>
        <div class="meta-list">
          <div class="meta-row"><span>限购方式</span><strong>{{ detail.purchaseLimitType === 'SINGLE' ? '单人一次' : '单人多次' }} / {{ detail.purchaseLimitCount }}</strong></div>
          <div class="meta-row"><span>兑换码来源</span><strong>{{ getCodeSourceModeLabel(detail.codeSourceMode) }}</strong></div>
          <div class="meta-row"><span>发布模式</span><strong>{{ getPublishModeLabel(detail.publishMode) }}</strong></div>
          <div class="meta-row"><span>发布时间</span><strong>{{ formatDisplayDateTime(detail.publishTime) }}</strong></div>
          <div class="meta-row"><span>活动开始</span><strong>{{ formatDisplayDateTime(detail.startTime) }}</strong></div>
          <div class="meta-row"><span>活动结束</span><strong>{{ formatDisplayDateTime(detail.endTime) }}</strong></div>
        </div>
      </article>
    </section>

    <ImportBatchPanel
      v-if="detail && shouldShowCodeImportPanel(detail)"
      :activity-id="activityId"
      :detail="detail"
      @detail-refreshed="detail = $event"
      @view-batch-detail="handleViewBatchDetail"
    />

    <section class="flat-panel flat-panel--soft" v-else-if="detail">
      <div class="eyebrow">Redeem Codes</div>
      <h2 class="import-static-title">当前活动使用系统生成兑换码</h2>
    </section>

    <section class="detail-actions" v-if="detail">
      <button class="flat-button flat-button--ghost" type="button" @click="router.push(`/admin/activities/${detail.id}/orders`)">
        <ReceiptText :size="18" />
        订单与兑换码
      </button>
      <button v-if="isEditableActivity(detail)" class="flat-button flat-button--secondary" type="button" @click="router.push(`/admin/activities/${detail.id}/edit`)">
        <PenSquare :size="18" />
        编辑活动
      </button>
      <button v-if="detail.publishStatus === 'UNPUBLISHED'" class="flat-button" type="button" @click="handlePublish">
        <Megaphone :size="18" />
        {{ publishActionLabel }}
      </button>
      <button v-if="detail.publishStatus !== 'OFFLINE'" class="flat-button flat-button--ghost" type="button" @click="handleOffline">
        <SquareArrowOutUpRight :size="18" />
        下线活动
      </button>
    </section>

    <ImportBatchDialog
      v-model:visible="batchDialogVisible"
      :activity-id="activityId"
      :batch-no="selectedBatchNo"
    />
  </div>
</template>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.import-static-title {
  margin: 0.25rem 0 0;
  font-size: 1.5rem;
}

@media (max-width: 960px) {
  .detail-grid {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
