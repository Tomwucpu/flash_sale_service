<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ExternalLink, RefreshCw } from 'lucide-vue-next'
import { orderApi } from '@/api/order'
import { ApiClientError } from '@/api/request'
import OrderStatusBadges from '@/components/OrderStatusBadges.vue'
import type { OrderDetail } from '@/types'
import { formatDisplayDateTime } from '@/utils/date'
import { formatOrderAmount, summarizeActivityOrders } from '@/utils/order'

const router = useRouter()
const loading = ref(false)
const orders = ref<OrderDetail[]>([])

const hasOrders = computed(() => orders.value.length > 0)
const summary = computed(() => {
  const items = orders.value
  const result = summarizeActivityOrders(items)
  return {
    total: result.total,
    success: result.confirmed,
    waitingPayment: result.waitingPayment,
  }
})

async function loadOrders() {
  loading.value = true
  try {
    orders.value = await orderApi.listMine()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '订单列表加载失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function viewActivity(activityId: number) {
  router.push(`/activities/${activityId}`)
}

onMounted(loadOrders)
</script>

<template>
  <div class="page-shell">
    <section class="page-header page-header--green">
      <div class="eyebrow">Orders</div>
      <h1 class="poster-title">我的订单</h1>
      <div class="stat-grid">
        <article class="stat-block">
          <strong>{{ summary.total }}</strong>
          <span>全部订单</span>
        </article>
        <article class="stat-block" style="background: #dbeafe">
          <strong>{{ summary.success }}</strong>
          <span>已完成</span>
        </article>
        <article class="stat-block" style="background: #fef3c7">
          <strong>{{ summary.waitingPayment }}</strong>
          <span>待支付</span>
        </article>
      </div>
    </section>

    <section class="flat-panel orders-table-panel">
      <div class="list-toolbar">
        <div>
          <div class="eyebrow">Purchase History</div>
          <h2>购买记录</h2>
        </div>
        <button class="flat-button flat-button--ghost" type="button" :disabled="loading" @click="loadOrders">
          <RefreshCw :size="18" />
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>

      <div v-loading="loading" class="orders-table-loading">
        <div v-if="hasOrders" class="orders-table-wrap">
          <el-table class="orders-table" :data="orders" row-key="orderNo">
            <el-table-column prop="orderNo" label="订单" min-width="200" sortable>
              <template #default="{ row }">
                <div class="cell-stack">
                  <strong>{{ row.orderNo }}</strong>
                  <span>活动 #{{ row.activityId }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="priceAmount" label="金额" width="120" sortable>
              <template #default="{ row }">
                <strong>{{ formatOrderAmount(row.priceAmount) }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="状态" min-width="250">
              <template #default="{ row }">
                <OrderStatusBadges :order-status="row.orderStatus" :pay-status="row.payStatus" :code-status="row.codeStatus" />
              </template>
            </el-table-column>
            <el-table-column label="兑换码" min-width="190">
              <template #default="{ row }">
                <div class="cell-stack">
                  <strong class="code-value">{{ row.code || '-' }}</strong>
                  <span>{{ row.failReason || '无异常' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" min-width="160" sortable>
              <template #default="{ row }">
                {{ formatDisplayDateTime(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <button class="flat-button flat-button--ghost action-button" type="button" @click="viewActivity(row.activityId)">
                  <ExternalLink :size="16" />
                  活动
                </button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="empty-state">
          <strong>暂无订单记录</strong>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.list-toolbar h2 {
  margin: 0.25rem 0 0;
}

.orders-table-panel,
.orders-table-loading,
.orders-table-wrap {
  min-width: 0;
}

.orders-table-panel {
  overflow: hidden;
}

.orders-table-wrap {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}

.orders-table {
  width: 100%;
}

.cell-stack {
  display: grid;
  gap: 0.25rem;
}

.cell-stack span {
  color: var(--fg-soft);
  font-size: 0.85rem;
}

.code-value {
  word-break: break-all;
}

.action-button {
  min-height: 42px;
  padding: 0.55rem 0.8rem;
}

.empty-state {
  display: grid;
  place-items: center;
  min-height: 220px;
  border: 2px dashed var(--fg);
  background: var(--muted);
}

@media (max-width: 760px) {
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
