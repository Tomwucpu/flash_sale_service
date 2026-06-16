<script setup lang="ts">
import { computed } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import StatusBadge from '@/components/StatusBadge.vue'
import { formatDisplayDateTime } from '@/utils/date'
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
import type { OrderDetail } from '@/types'

const props = defineProps<{
  activityOrders: OrderDetail[]
  orderQuerying: boolean
  isAuthenticated: boolean
}>()

const emit = defineEmits<{
  'query-orders': []
}>()

const hasActivityOrders = computed(() => props.activityOrders.length > 0)
const activityOrderSummary = computed(() => summarizeActivityOrders(props.activityOrders))
</script>

<template>
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
        <button class="flat-button flat-button--ghost" type="button" :disabled="orderQuerying" @click="emit('query-orders')">
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
</template>

<style scoped>
.order-panel__header,
.order-panel__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.order-panel__title {
  margin: 0.25rem 0 0;
  font-size: 1.35rem;
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
  .order-panel__header,
  .order-panel__actions {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
