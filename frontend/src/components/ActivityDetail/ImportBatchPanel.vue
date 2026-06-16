<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from 'lucide-vue-next'
import { activityApi } from '@/api/activity'
import { ApiClientError } from '@/api/request'
import StatusBadge from '@/components/StatusBadge.vue'
import { getImportFailureReasonLabel, getPublishStatusLabel } from '@/utils/activity'
import type { ActivityDetail, RedeemCodeImportBatchDetail, RedeemCodeImportBatchSummary } from '@/types'

const props = defineProps<{
  activityId: number
  detail: ActivityDetail
}>()

const emit = defineEmits<{
  'detail-refreshed': [detail: ActivityDetail]
  'view-batch-detail': [batchNo: string]
}>()

const importSubmitting = ref(false)
const importBatchesLoading = ref(false)
const importBatches = ref<RedeemCodeImportBatchSummary[]>([])
const latestImportResult = ref<RedeemCodeImportBatchDetail | null>(null)
const selectedImportFile = ref<File | null>(null)

const canImportCodes = computed(() => props.detail.publishStatus === 'UNPUBLISHED')

async function loadImportBatches() {
  importBatchesLoading.value = true
  try {
    importBatches.value = await activityApi.listImportBatches(props.activityId)
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '导入批次加载失败'
    ElMessage.error(message)
  } finally {
    importBatchesLoading.value = false
  }
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
    latestImportResult.value = await activityApi.importCodes(props.activityId, selectedImportFile.value)
    ElMessage.success('兑换码导入完成')
    selectedImportFile.value = null
    await loadImportBatches()
    const refreshedDetail = await activityApi.detail(props.activityId)
    emit('detail-refreshed', refreshedDetail)
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '兑换码导入失败'
    ElMessage.error(message)
  } finally {
    importSubmitting.value = false
  }
}

onMounted(loadImportBatches)
</script>

<template>
  <section class="flat-panel flat-panel--blue">
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
          <strong>{{ latestImportResult.fileName }}</strong> <span class="text-muted ml-2">{{ latestImportResult.batchNo }}</span>
        </div>
        <div class="batch-stats-inline">
          <span>总数: <strong>{{ latestImportResult.totalCount }}</strong></span>
          <span>成功: <strong style="color: var(--color-green, #16a34a)">{{ latestImportResult.successCount }}</strong></span>
          <span>失败: <strong style="color: var(--color-red, #dc2626)">{{ latestImportResult.failedCount }}</strong></span>
        </div>
      </div>
      <div class="failure-list" v-if="latestImportResult.failures.length > 0">
        <div class="failure-item" v-for="failure in latestImportResult.failures" :key="`${latestImportResult.batchNo}-${failure.lineNumber}`">
          <div class="failure-item__meta">
            <span class="text-muted">第 {{ failure.lineNumber }} 行：</span>
            <strong>{{ failure.rawCode || '空值' }}</strong>
          </div>
          <span class="failure-reason">{{ getImportFailureReasonLabel(failure.reason) }}</span>
        </div>
      </div>
    </article>

    <div class="batch-list" v-loading="importBatchesLoading">
      <div class="batch-list-header">
        <div class="eyebrow">Batch History</div>
      </div>

      <div v-if="importBatches.length > 0" class="batch-list-body">
        <article class="batch-card" v-for="batch in importBatches" :key="batch.batchNo">
          <div class="batch-card-header">
            <div class="batch-title">
              <strong>{{ batch.fileName }}</strong>
              <span class="text-muted">{{ batch.batchNo }}</span>
            </div>
            <div class="batch-stats-inline">
              <span>总数: <strong>{{ batch.totalCount }}</strong></span>
              <span>成功: <strong style="color: var(--color-green, #16a34a)">{{ batch.successCount }}</strong></span>
              <span>失败: <strong style="color: var(--color-red, #dc2626)">{{ batch.failedCount }}</strong></span>
            </div>
            <button class="text-button" type="button" @click="emit('view-batch-detail', batch.batchNo)">
              查看明细
            </button>
          </div>
        </article>
      </div>
      <div v-else class="empty-state compact-empty-state">
        <strong>还没有导入记录</strong>
      </div>
    </div>
  </section>
</template>

<style scoped>
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

.import-panel-header h2 {
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
  margin-top: 1.5rem;
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

.import-tip {
  margin-top: 1rem;
}

.import-tip,
.batch-list-header span,
.batch-card-header span {
  color: var(--fg-soft);
}

.latest-import-panel {
  margin-top: 1.5rem;
}

.latest-import-panel,
.batch-list-body,
.failure-list {
  display: grid;
  gap: 1rem;
}

.batch-list {
  display: grid;
  gap: 1.25rem;
  margin-top: 1.5rem;
}

.batch-card {
  padding: 1.25rem 1.5rem;
  border: 2px solid rgba(17, 24, 39, 0.12);
  background: white;
  transition: border-color 0.2s ease;
}
.batch-card:hover {
  border-color: var(--fg);
}

.batch-card-header {
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1.5rem;
}

.batch-title {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
  min-width: 250px;
}

.text-muted {
  color: var(--fg-soft);
  font-size: 0.95rem;
}

.ml-2 {
  margin-left: 0.75rem;
}

.batch-stats-inline {
  display: flex;
  align-items: center;
  gap: 2rem;
  font-size: 1.05rem;
}

.text-button {
  background: none;
  border: none;
  padding: 0;
  color: #2563eb;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  font-size: 1rem;
}

.text-button:hover {
  color: #1d4ed8;
}

.failure-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.25rem;
  border-left: 4px solid #ef4444;
  background: rgba(254, 242, 242, 0.5);
  font-size: 0.95rem;
  margin-bottom: 0.5rem;
}

.failure-item:last-child {
  margin-bottom: 0;
}

.failure-item__meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.failure-reason {
  color: #dc2626;
  font-weight: 500;
}

.compact-empty-state {
  padding: 1rem;
}

@media (max-width: 960px) {
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
