<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Download, LoaderCircle, RefreshCw } from 'lucide-vue-next'
import { activityApi } from '@/api/activity'
import { exportApi } from '@/api/export'
import { orderApi } from '@/api/order'
import { ApiClientError } from '@/api/request'
import StatusBadge from '@/components/StatusBadge.vue'
import type { ActivityDetail, OrderDetail } from '@/types'
import { formatDisplayDateTime } from '@/utils/date'
import {
  buildSoldCodeExportPayload,
  canExportSoldCodes,
  getExportFileName,
} from '@/utils/export-task'
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

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const exporting = ref(false)
const detail = ref<ActivityDetail | null>(null)
const orders = ref<OrderDetail[]>([])

const activityId = computed(() => Number(route.params.id))
const hasOrders = computed(() => orders.value.length > 0)
const hasSoldCodeExports = computed(() => canExportSoldCodes(orders.value))
const summary = computed(() => summarizeActivityOrders(orders.value))

async function loadOrders() {
  loading.value = true
  try {
    const [activityDetail, activityOrders] = await Promise.all([
      activityApi.detail(activityId.value),
      orderApi.listActivityOrdersForPublisher(activityId.value),
    ])
    detail.value = activityDetail
    orders.value = activityOrders
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '活动订单加载失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function wait(ms: number) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms)
  })
}

async function waitForExportTask(taskId: number) {
  for (let attempt = 0; attempt < 30; attempt += 1) {
    await wait(1000)
    const task = await exportApi.getTask(taskId)

    if (task.status === 'SUCCESS') {
      if (!task.fileUrl) {
        throw new Error('导出任务已完成，但未返回下载地址')
      }
      return task
    }

    if (task.status === 'FAILED') {
      throw new Error(task.failReason || '导出任务失败')
    }
  }

  throw new Error('导出任务超时，请稍后重试')
}

function downloadBlob(blob: Blob, fileName: string) {
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(objectUrl)
}

async function handleExportSoldCodes() {
  if (!hasSoldCodeExports.value) {
    ElMessage.warning('暂无已售兑换码可导出')
    return
  }

  exporting.value = true
  try {
    const task = await exportApi.createTask(buildSoldCodeExportPayload(activityId.value))
    const completedTask = task.status === 'SUCCESS' ? task : await waitForExportTask(task.id)

    if (!completedTask.fileUrl) {
      throw new Error('导出任务未返回下载地址')
    }

    const fileName = getExportFileName(completedTask.fileUrl, `activity-${activityId.value}-sold-codes.csv`)
    const blob = await exportApi.downloadFile(fileName)

    downloadBlob(blob, fileName)
    ElMessage.success('导出文件已下载')
  } catch (error) {
    const message = error instanceof ApiClientError || error instanceof Error ? error.message : '导出失败'
    ElMessage.error(message)
  } finally {
    exporting.value = false
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="page-shell">
    <section class="page-header page-header--green">
      <div class="eyebrow">Activity Orders</div>
      <h1 class="poster-title">订单与兑换码</h1>
      <p class="poster-copy" v-if="detail">{{ detail.title }} · 活动 #{{ detail.id }}</p>
      <div class="order-stat-grid">
        <article class="stat-block">
          <strong>{{ summary.total }}</strong>
          <span>抢购订单</span>
        </article>
        <article class="stat-block" style="background: #dbeafe">
          <strong>{{ summary.confirmed }}</strong>
          <span>已确认</span>
        </article>
        <article class="stat-block" style="background: #d1fae5">
          <strong>{{ summary.issuedCodes }}</strong>
          <span>已发码</span>
        </article>
        <article class="stat-block" style="background: #fef3c7">
          <strong>{{ summary.waitingPayment }}</strong>
          <span>待支付</span>
        </article>
        <article class="stat-block" style="background: #e5e7eb">
          <strong>{{ summary.closedOrFailed }}</strong>
          <span>关闭/失败</span>
        </article>
      </div>
    </section>

    <section class="flat-panel orders-table-panel">
      <div class="list-toolbar">
        <div>
          <div class="eyebrow">Records</div>
          <h2>抢购记录</h2>
        </div>
        <div class="toolbar-actions">
          <button class="flat-button flat-button--ghost" type="button" @click="router.push(`/admin/activities/${activityId}`)">
            <ArrowLeft :size="18" />
            活动详情
          </button>
          <button
            class="flat-button flat-button--secondary"
            type="button"
            :disabled="loading || exporting || !hasSoldCodeExports"
            :title="hasSoldCodeExports ? '导出已售兑换码' : '暂无已售兑换码可导出'"
            @click="handleExportSoldCodes"
          >
            <LoaderCircle v-if="exporting" class="spin-icon" :size="18" />
            <Download v-else :size="18" />
            {{ exporting ? '导出中...' : '导出已售兑换码' }}
          </button>
          <button class="flat-button" type="button" :disabled="loading" @click="loadOrders">
            <RefreshCw :size="18" />
            {{ loading ? '刷新中...' : '刷新' }}
          </button>
        </div>
      </div>

      <div v-loading="loading" class="orders-table-loading">
        <div v-if="hasOrders" class="orders-table-wrap">
          <el-table class="orders-table" :data="orders" row-key="orderNo">
            <el-table-column prop="orderNo" label="订单" min-width="210">
              <template #default="{ row }">
                <div class="cell-stack">
                  <strong>{{ row.orderNo }}</strong>
                  <span>用户 #{{ row.userId }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="120">
              <template #default="{ row }">
                <strong>{{ formatOrderAmount(row.priceAmount) }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="状态" min-width="280">
              <template #default="{ row }">
                <div class="badge-stack">
                  <StatusBadge :label="getOrderStatusLabel(row.orderStatus)" :tone="orderStatusTone(row.orderStatus)" />
                  <StatusBadge :label="getPayStatusLabel(row.payStatus)" :tone="payStatusTone(row.payStatus)" />
                  <StatusBadge :label="getCodeStatusLabel(row.codeStatus)" :tone="codeStatusTone(row.codeStatus)" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="兑换码" min-width="220">
              <template #default="{ row }">
                <div class="cell-stack">
                  <strong class="code-value">{{ row.code || '-' }}</strong>
                  <span>{{ row.failReason || '无异常' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="170">
              <template #default="{ row }">
                {{ formatDisplayDateTime(row.updatedAt) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="empty-state">
          <strong>当前活动暂无抢购订单</strong>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.order-stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 1rem;
}

.list-toolbar,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.list-toolbar {
  justify-content: space-between;
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

.badge-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.spin-icon {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.code-value {
  word-break: break-all;
}

.empty-state {
  display: grid;
  place-items: center;
  min-height: 220px;
  border: 2px dashed var(--fg);
  background: var(--muted);
}

@media (max-width: 1180px) {
  .order-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .order-stat-grid,
  .list-toolbar,
  .toolbar-actions {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
