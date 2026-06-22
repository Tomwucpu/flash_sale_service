<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Clock, Search, ShieldCheck, X } from 'lucide-vue-next'
import { applicationApi } from '@/api/application'
import { ApiClientError } from '@/api/request'
import type { ApplicationPageResponse, PublisherApplication } from '@/types'

const loading = ref(false)
const data = ref<ApplicationPageResponse | null>(null)
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const reviewNoteMap = ref<Record<number, string>>({})
const reviewingId = ref<number | null>(null)

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待审批' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已拒绝' },
]

const statusMap: Record<string, { label: string; tone: string }> = {
  PENDING: { label: '待审批', tone: 'warning' },
  APPROVED: { label: '已通过', tone: 'success' },
  REJECTED: { label: '已拒绝', tone: 'danger' },
}

function badgeClass(status: string) {
  const tone = statusMap[status]?.tone ?? 'default'
  return `status-badge status-badge--${tone}`
}

async function loadData() {
  loading.value = true
  try {
    data.value = await applicationApi.list({
      status: statusFilter.value || undefined,
      page: currentPage.value,
      size: pageSize.value,
    })
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function handleApprove(app: PublisherApplication) {
  try {
    await ElMessageBox.confirm('确定批准该用户的发布者申请吗？', '确认批准', {
      confirmButtonText: '批准',
      cancelButtonText: '取消',
      type: 'info',
    })
  } catch {
    return
  }

  reviewingId.value = app.id
  try {
    await applicationApi.approve(app.id, {
      reviewNote: reviewNoteMap.value[app.id] || undefined,
    })
    ElMessage.success('已批准')
    reviewNoteMap.value[app.id] = ''
    await loadData()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '操作失败'
    ElMessage.error(message)
  } finally {
    reviewingId.value = null
  }
}

async function handleReject(app: PublisherApplication) {
  try {
    await ElMessageBox.confirm('确定拒绝该用户的发布者申请吗？', '确认拒绝', {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  reviewingId.value = app.id
  try {
    await applicationApi.reject(app.id, {
      reviewNote: reviewNoteMap.value[app.id] || undefined,
    })
    ElMessage.success('已拒绝')
    reviewNoteMap.value[app.id] = ''
    await loadData()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '操作失败'
    ElMessage.error(message)
  } finally {
    reviewingId.value = null
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadData()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadData()
}

function handleFilterChange() {
  currentPage.value = 1
  loadData()
}

function formatTime(val: string | null) {
  if (!val) return '-'
  return new Date(val).toLocaleString()
}

onMounted(loadData)
</script>

<template>
  <div class="page-grid">
    <section class="page-header page-header--blue">
      <div class="eyebrow">Publisher Applications</div>
      <h1>发布者申请管理</h1>
      <p>审核用户提交的发布者申请，批准后用户将获得发布者权限。</p>
    </section>

    <section class="flat-panel">
      <div class="list-toolbar">
        <div>
          <div class="eyebrow">Review</div>
          <h2>申请列表</h2>
        </div>
        <span v-if="data" class="list-stats">共 {{ data.total }} 条记录</span>
      </div>

      <div class="filter-bar">
        <div class="filter-search">
          <ShieldCheck :size="16" />
          <span>筛选状态</span>
        </div>
        <select v-model="statusFilter" class="filter-select" @change="handleFilterChange">
          <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
      </div>

      <div v-loading="loading" class="table-container">
        <el-table v-if="data && data.records.length > 0" :data="data.records" row-key="id" style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" sortable />
          <el-table-column prop="username" label="申请人" min-width="120" sortable>
            <template #default="{ row }">
              <strong>{{ row.username }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="申请理由" min-width="240">
            <template #default="{ row }">
              <div class="reason-cell">{{ row.reason }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <span :class="badgeClass(row.status)">
                {{ statusMap[row.status]?.label }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="170" sortable>
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="reviewNote" label="审核意见" min-width="160">
            <template #default="{ row }">
              <span v-if="row.reviewNote">{{ row.reviewNote }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="reviewerName" label="审核人" width="100">
            <template #default="{ row }">
              <span v-if="row.reviewerName">{{ row.reviewerName }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <div class="action-cell">
                  <el-input
                    v-model="reviewNoteMap[row.id]"
                    size="small"
                    placeholder="审核意见（可选）"
                    maxlength="200"
                    class="action-note"
                  />
                  <el-button
                    type="success"
                    size="small"
                    :loading="reviewingId === row.id"
                    @click="handleApprove(row)"
                  >
                    <Check :size="14" />
                    批准
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    :loading="reviewingId === row.id"
                    @click="handleReject(row)"
                  >
                    <X :size="14" />
                    拒绝
                  </el-button>
                </div>
              </template>
              <span v-else class="text-muted">已处理</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-else-if="!loading" class="empty-state">
          <ShieldCheck :size="40" />
          <p>暂无申请记录</p>
        </div>
      </div>

      <div v-if="data && data.total > 0" class="list-pagination">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="data.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-grid {
  display: grid;
  gap: 1.25rem;
}

.page-grid h1 {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 800;
}

.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem 0;
}

.list-toolbar h2 {
  margin: 0.25rem 0 0;
  font-size: 1.15rem;
  font-weight: 700;
}

.list-stats {
  color: var(--fg-soft);
  font-size: 0.85rem;
  font-weight: 600;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid var(--border);
}

.filter-search {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--fg-soft);
  font-size: 0.85rem;
  font-weight: 600;
}

.filter-select {
  padding: 0.4rem 0.75rem;
  border: 2px solid var(--border);
  border-radius: 0;
  background: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.table-container {
  min-height: 200px;
}

.reason-cell {
  max-width: 280px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.text-muted {
  color: var(--fg-soft);
  font-size: 0.85rem;
}

/* Status badges */
.status-badge {
  display: inline-block;
  padding: 0.2rem 0.6rem;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  border: 2px solid;
}

.status-badge--warning {
  color: #b45309;
  border-color: #f59e0b;
  background: #fef3c7;
}

.status-badge--success {
  color: #047857;
  border-color: #10b981;
  background: #d1fae5;
}

.status-badge--danger {
  color: #b91c1c;
  border-color: #ef4444;
  background: #fee2e2;
}

/* Action cell */
.action-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.action-note {
  flex: 1;
  min-width: 100px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 3rem 1rem;
  color: var(--fg-soft);
}

.empty-state p {
  margin: 0;
  font-size: 0.9rem;
}

.list-pagination {
  display: flex;
  justify-content: center;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--border);
}
</style>
