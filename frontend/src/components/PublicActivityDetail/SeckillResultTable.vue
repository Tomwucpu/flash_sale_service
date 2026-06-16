<script setup lang="ts">
import StatusBadge from '@/components/StatusBadge.vue'
import { formatDisplayDateTime } from '@/utils/date'
import type { SeckillResult } from '@/types'

defineProps<{
  seckillResult: SeckillResult | null
}>()

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
  if (status === 'SUCCESS') return 'green'
  if (status === 'PROCESSING') return 'blue'
  if (status === 'PENDING_PAYMENT') return 'amber'
  return 'slate'
}
</script>

<template>
  <article class="flat-panel seckill-result" v-if="seckillResult">
    <div class="seckill-result__header">
      <div>
        <div class="eyebrow">Result</div>
        <h3 class="seckill-result__title">抢购结果</h3>
      </div>
    </div>
    <div class="seckill-result-table-wrap">
      <el-table class="seckill-result-table" :data="[seckillResult]" row-key="status">
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
</template>

<style scoped>
.seckill-result__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.seckill-result__title {
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

.order-inline-cell,
.order-no,
.code-value {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 960px) {
  .seckill-result__header {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
