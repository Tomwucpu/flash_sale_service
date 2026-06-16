<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleAlert } from 'lucide-vue-next'
import { activityApi } from '@/api/activity'
import { ApiClientError } from '@/api/request'
import { getImportFailureReasonLabel } from '@/utils/activity'
import type { RedeemCodeImportBatchDetail } from '@/types'

const props = defineProps<{
  visible: boolean
  activityId: number
  batchNo: string | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const loading = ref(false)
const batchDetail = ref<RedeemCodeImportBatchDetail | null>(null)

async function loadBatchDetail(batchNo: string) {
  loading.value = true
  try {
    batchDetail.value = await activityApi.importBatchDetail(props.activityId, batchNo)
  } catch (error) {
    emit('update:visible', false)
    const message = error instanceof ApiClientError ? error.message : '批次详情加载失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.batchNo,
  (batchNo) => {
    if (batchNo) {
      batchDetail.value = null
      loadBatchDetail(batchNo)
    }
  },
)

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      batchDetail.value = null
    }
  },
)
</script>

<template>
  <el-dialog :model-value="visible" title="导入批次详情" width="720px" @update:model-value="emit('update:visible', $event)">
    <div v-loading="loading" v-if="batchDetail" class="dialog-stack">
      <div class="meta-list">
        <div class="meta-row"><span>批次号</span><strong>{{ batchDetail.batchNo }}</strong></div>
        <div class="meta-row"><span>文件名</span><strong>{{ batchDetail.fileName }}</strong></div>
        <div class="meta-row"><span>总行数</span><strong>{{ batchDetail.totalCount }}</strong></div>
        <div class="meta-row"><span>成功导入</span><strong>{{ batchDetail.successCount }}</strong></div>
        <div class="meta-row"><span>失败行</span><strong>{{ batchDetail.failedCount }}</strong></div>
      </div>

      <div class="failure-list" v-if="batchDetail.failures.length > 0">
        <div class="failure-item" v-for="failure in batchDetail.failures" :key="`${batchDetail.batchNo}-${failure.lineNumber}`">
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
</template>

<style scoped>
.dialog-stack,
.failure-list {
  display: grid;
  gap: 1rem;
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

.compact-empty-state {
  padding: 1rem;
}

.dialog-placeholder {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--fg-soft);
}

@media (max-width: 960px) {
  .failure-item {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
