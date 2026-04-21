<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleAlert, History, Megaphone, PenSquare, SquareArrowOutUpRight, Upload } from 'lucide-vue-next'
import { activityApi } from '@/api/activity'
import { ApiClientError } from '@/api/request'
import StatusBadge from '@/components/StatusBadge.vue'
import {
  getCodeSourceModeLabel,
  getImportFailureReasonLabel,
  getPhaseLabel,
  getPublishModeLabel,
  getPublishStatusLabel,
  isEditableActivity,
  shouldShowCodeImportPanel,
} from '@/utils/activity'
import { formatDisplayDateTime } from '@/utils/date'
import type { ActivityDetail, RedeemCodeImportBatchDetail, RedeemCodeImportBatchSummary } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const importSubmitting = ref(false)
const importBatchesLoading = ref(false)
const batchDetailLoading = ref(false)
const detail = ref<ActivityDetail | null>(null)
const importBatches = ref<RedeemCodeImportBatchSummary[]>([])
const latestImportResult = ref<RedeemCodeImportBatchDetail | null>(null)
const selectedBatchDetail = ref<RedeemCodeImportBatchDetail | null>(null)
const selectedImportFile = ref<File | null>(null)
const batchDialogVisible = ref(false)

const activityId = computed(() => Number(route.params.id))
const canImportCodes = computed(() => detail.value?.publishStatus === 'UNPUBLISHED')
const isAdvancePublish = computed(() => detail.value?.publishMode === 'SCHEDULED')
const publishActionLabel = computed(() => (isAdvancePublish.value ? '提前发布活动' : '立即发布活动'))

async function loadImportBatches() {
  if (!detail.value || !shouldShowCodeImportPanel(detail.value)) {
    importBatches.value = []
    return
  }

  importBatchesLoading.value = true
  try {
    importBatches.value = await activityApi.listImportBatches(activityId.value)
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '导入批次加载失败'
    ElMessage.error(message)
  } finally {
    importBatchesLoading.value = false
  }
}

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await activityApi.detail(activityId.value)
    if (shouldShowCodeImportPanel(detail.value)) {
      await loadImportBatches()
    } else {
      importBatches.value = []
      latestImportResult.value = null
    }
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

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  selectedImportFile.value = target.files?.[0] ?? null
}

async function handleImportCodes() {
  if (!selectedImportFile.value) {
    ElMessage.error('请先选择 csv 或 xlsx 文件')
    return
  }

  importSubmitting.value = true
  try {
    latestImportResult.value = await activityApi.importCodes(activityId.value, selectedImportFile.value)
    ElMessage.success('兑换码导入完成')
    selectedImportFile.value = null
    await loadImportBatches()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '兑换码导入失败'
    ElMessage.error(message)
  } finally {
    importSubmitting.value = false
  }
}

async function handleViewBatchDetail(batchNo: string) {
  batchDetailLoading.value = true
  batchDialogVisible.value = true
  try {
    selectedBatchDetail.value = await activityApi.importBatchDetail(activityId.value, batchNo)
  } catch (error) {
    batchDialogVisible.value = false
    const message = error instanceof ApiClientError ? error.message : '批次详情加载失败'
    ElMessage.error(message)
  } finally {
    batchDetailLoading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="page-header page-header--amber" v-if="detail">
      <div class="eyebrow">Activity Detail</div>
      <h1 class="poster-title">{{ detail.title }}</h1>
      <p class="poster-copy">{{ detail.description || '当前活动没有填写补充描述。' }}</p>
      <div class="badge-stack">
        <StatusBadge
          :label="getPublishStatusLabel(detail.publishStatus)"
          :tone="detail.publishStatus === 'PUBLISHED' ? 'green' : detail.publishStatus === 'UNPUBLISHED' ? 'amber' : 'slate'"
        />
        <StatusBadge
          :label="getPhaseLabel(detail.phase)"
          :tone="detail.phase === 'ONGOING' ? 'blue' : detail.phase === 'PREVIEW' ? 'amber' : 'slate'"
        />
      </div>
    </section>

    <section class="detail-grid" v-if="detail">
      <article class="flat-panel">
        <div class="eyebrow">Overview</div>
        <div class="meta-list">
          <div class="meta-row"><span>活动 ID</span><strong>#{{ detail.id }}</strong></div>
          <div class="meta-row"><span>封面图</span><strong>{{ detail.coverUrl || '未设置' }}</strong></div>
          <div class="meta-row"><span>库存</span><strong>{{ detail.availableStock }} / {{ detail.totalStock }}</strong></div>
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

    <section class="flat-panel flat-panel--blue" v-if="detail && shouldShowCodeImportPanel(detail)">
      <div class="import-panel-header">
        <div>
          <div class="eyebrow">Redeem Codes</div>
          <h2>兑换码导入与批次记录</h2>
        </div>
        <div class="import-panel-badges">
          <StatusBadge :label="canImportCodes ? '当前可导入' : '当前不可导入'" :tone="canImportCodes ? 'green' : 'slate'" />
          <StatusBadge :label="`历史批次 ${importBatches.length}`" tone="blue" />
        </div>
      </div>

      <div class="import-toolbar">
        <label class="import-file-picker">
          <Upload :size="18" />
          <span>{{ selectedImportFile ? selectedImportFile.name : '选择兑换码文件' }}</span>
          <input accept=".csv,.xlsx" class="visually-hidden" type="file" @change="handleFileChange" />
        </label>
        <button class="flat-button" type="button" :disabled="!canImportCodes || importSubmitting" @click="handleImportCodes">
          <Upload :size="18" />
          {{ importSubmitting ? '导入中...' : '导入兑换码' }}
        </button>
      </div>

      <p class="import-tip" v-if="!canImportCodes">
        当前状态：{{ getPublishStatusLabel(detail.publishStatus) }}。仅未发布活动可导入兑换码。
      </p>

      <article class="flat-panel latest-import-panel" v-if="latestImportResult">
        <div class="batch-card-header">
          <div>
            <div class="eyebrow">Latest Batch</div>
            <strong>{{ latestImportResult.fileName }}</strong>
          </div>
          <span>{{ latestImportResult.batchNo }}</span>
        </div>
        <div class="flat-grid flat-grid--3">
          <div class="mini-stat">
            <span>总行数</span>
            <strong>{{ latestImportResult.totalCount }}</strong>
          </div>
          <div class="mini-stat">
            <span>成功导入</span>
            <strong>{{ latestImportResult.successCount }}</strong>
          </div>
          <div class="mini-stat">
            <span>失败行</span>
            <strong>{{ latestImportResult.failedCount }}</strong>
          </div>
        </div>
        <div class="failure-list" v-if="latestImportResult.failures.length > 0">
          <div class="failure-item" v-for="failure in latestImportResult.failures" :key="`${latestImportResult.batchNo}-${failure.lineNumber}`">
            <div class="failure-item__meta">
              <span>第 {{ failure.lineNumber }} 行</span>
              <strong>{{ failure.rawCode || '空值' }}</strong>
            </div>
            <span>{{ getImportFailureReasonLabel(failure.reason) }}</span>
          </div>
        </div>
      </article>

      <div class="batch-list" v-loading="importBatchesLoading">
        <div class="batch-list-header">
          <div class="eyebrow">Batch History</div>
          <span>最新优先</span>
        </div>

        <div v-if="importBatches.length > 0" class="batch-list-body">
          <article class="batch-card" v-for="batch in importBatches" :key="batch.batchNo">
            <div class="batch-card-header">
              <div>
                <strong>{{ batch.fileName }}</strong>
                <span>{{ batch.batchNo }}</span>
              </div>
              <button class="flat-button flat-button--ghost batch-card__action" type="button" @click="handleViewBatchDetail(batch.batchNo)">
                <History :size="16" />
                查看明细
              </button>
            </div>
            <div class="flat-grid flat-grid--3">
              <div class="mini-stat">
                <span>总行数</span>
                <strong>{{ batch.totalCount }}</strong>
              </div>
              <div class="mini-stat">
                <span>成功导入</span>
                <strong>{{ batch.successCount }}</strong>
              </div>
              <div class="mini-stat">
                <span>失败行</span>
                <strong>{{ batch.failedCount }}</strong>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="empty-state compact-empty-state">
          <strong>还没有导入记录</strong>
        </div>
      </div>
    </section>

    <section class="flat-panel flat-panel--soft" v-else-if="detail">
      <div class="eyebrow">Redeem Codes</div>
      <h2 class="import-static-title">当前活动使用系统生成兑换码</h2>
    </section>

    <section class="detail-actions" v-if="detail">
      <button class="flat-button flat-button--secondary" type="button" :disabled="!isEditableActivity(detail)" @click="router.push(`/admin/activities/${detail.id}/edit`)">
        <PenSquare :size="18" />
        编辑活动
      </button>
      <button class="flat-button" type="button" :disabled="detail.publishStatus !== 'UNPUBLISHED'" @click="handlePublish">
        <Megaphone :size="18" />
        {{ publishActionLabel }}
      </button>
      <button class="flat-button flat-button--ghost" type="button" :disabled="detail.publishStatus === 'OFFLINE'" @click="handleOffline">
        <SquareArrowOutUpRight :size="18" />
        下线活动
      </button>
    </section>

    <el-dialog v-model="batchDialogVisible" title="导入批次详情" width="720px">
      <div v-loading="batchDetailLoading" v-if="selectedBatchDetail" class="dialog-stack">
        <div class="meta-list">
          <div class="meta-row"><span>批次号</span><strong>{{ selectedBatchDetail.batchNo }}</strong></div>
          <div class="meta-row"><span>文件名</span><strong>{{ selectedBatchDetail.fileName }}</strong></div>
          <div class="meta-row"><span>总行数</span><strong>{{ selectedBatchDetail.totalCount }}</strong></div>
          <div class="meta-row"><span>成功导入</span><strong>{{ selectedBatchDetail.successCount }}</strong></div>
          <div class="meta-row"><span>失败行</span><strong>{{ selectedBatchDetail.failedCount }}</strong></div>
        </div>

        <div class="failure-list" v-if="selectedBatchDetail.failures.length > 0">
          <div class="failure-item" v-for="failure in selectedBatchDetail.failures" :key="`${selectedBatchDetail.batchNo}-${failure.lineNumber}`">
            <div class="failure-item__meta">
              <span>第 {{ failure.lineNumber }} 行</span>
              <strong>{{ failure.rawCode || '空值' }}</strong>
            </div>
            <span>{{ getImportFailureReasonLabel(failure.reason) }}</span>
          </div>
        </div>
        <div v-else class="empty-state compact-empty-state">
          <strong>这个批次没有失败记录</strong>
        </div>
      </div>
      <div v-else class="dialog-placeholder">
        <CircleAlert :size="18" />
        <span>请选择一个导入批次查看详情。</span>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.badge-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.import-panel-header,
.batch-card-header,
.batch-list-header,
.failure-item,
.failure-item__meta,
.import-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.import-panel-header,
.batch-list-header {
  align-items: center;
}

.import-panel-header h2,
.import-static-title {
  margin: 0.25rem 0 0;
  font-size: 1.5rem;
}

.import-panel-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.import-toolbar {
  align-items: center;
  flex-wrap: wrap;
}

.import-file-picker {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  min-height: 56px;
  padding: 0.95rem 1.2rem;
  border: 2px dashed var(--fg);
  background: white;
  font-weight: 700;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.import-tip,
.batch-list-header span,
.batch-card-header span,
.dialog-placeholder {
  color: var(--fg-soft);
}

.latest-import-panel,
.batch-list-body,
.dialog-stack,
.failure-list {
  display: grid;
  gap: 1rem;
}

.batch-list {
  display: grid;
  gap: 1rem;
  margin-top: 1.25rem;
}

.batch-card {
  padding: 1rem;
  border: 2px solid var(--fg);
  background: white;
}

.batch-card-header {
  align-items: start;
}

.batch-card-header strong,
.failure-item__meta strong {
  display: block;
}

.batch-card__action {
  min-height: 42px;
  padding: 0.55rem 0.8rem;
}

.mini-stat {
  padding: 0.85rem 1rem;
  border: 2px solid var(--fg);
  background: rgba(255, 255, 255, 0.85);
}

.mini-stat span {
  display: block;
  color: var(--fg-soft);
  font-size: 0.8rem;
}

.mini-stat strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.35rem;
}

.failure-item {
  align-items: center;
  padding: 0.85rem 1rem;
  border: 2px solid rgba(17, 24, 39, 0.12);
  background: white;
}

.failure-item__meta {
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
}

.compact-empty-state {
  padding: 1rem;
}

.dialog-placeholder {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

@media (max-width: 960px) {
  .detail-grid,
  .import-panel-header,
  .batch-card-header,
  .batch-list-header,
  .import-toolbar,
  .failure-item {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
