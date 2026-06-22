<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Clock, ShieldCheck, X } from 'lucide-vue-next'
import { applicationApi } from '@/api/application'
import { ApiClientError } from '@/api/request'
import StatusBadge from '@/components/StatusBadge.vue'
import type { ApplicationPageResponse, PublisherApplication } from '@/types'

const loading = ref(false)
const data = ref<ApplicationPageResponse | null>(null)
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// Dialog state
const dialogVisible = ref(false)
const selectedApp = ref<PublisherApplication | null>(null)
const reviewNote = ref('')
const reviewing = ref(false)

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待审批' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已拒绝' },
]

const statusToneMap: Record<string, 'amber' | 'green' | 'red'> = {
  PENDING: 'amber',
  APPROVED: 'green',
  REJECTED: 'red',
}

const statusLabelMap: Record<string, string> = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
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

function openDialog(app: PublisherApplication) {
  selectedApp.value = app
  reviewNote.value = ''
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  selectedApp.value = null
  reviewNote.value = ''
}

async function handleApprove() {
  if (!selectedApp.value) return
  try {
    await ElMessageBox.confirm('确定批准该用户的发布者申请吗？', '确认批准', {
      confirmButtonText: '批准',
      cancelButtonText: '取消',
      type: 'info',
    })
  } catch {
    return
  }

  reviewing.value = true
  try {
    await applicationApi.approve(selectedApp.value.id, {
      reviewNote: reviewNote.value || undefined,
    })
    ElMessage.success('已批准')
    closeDialog()
    await loadData()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '操作失败'
    ElMessage.error(message)
  } finally {
    reviewing.value = false
  }
}

async function handleReject() {
  if (!selectedApp.value) return
  try {
    await ElMessageBox.confirm('确定拒绝该用户的发布者申请吗？', '确认拒绝', {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  reviewing.value = true
  try {
    await applicationApi.reject(selectedApp.value.id, {
      reviewNote: reviewNote.value || undefined,
    })
    ElMessage.success('已拒绝')
    closeDialog()
    await loadData()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '操作失败'
    ElMessage.error(message)
  } finally {
    reviewing.value = false
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
  <div class="page-shell">
    <section class="page-header page-header--blue">
      <div class="eyebrow">Publisher Applications</div>
      <h1 class="poster-title">发布者申请管理</h1>
      <p class="poster-copy">审核用户提交的发布者申请，批准后用户将获得发布者权限。</p>
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
        <div class="filter-tags">
          <button
            v-for="opt in statusOptions"
            :key="opt.value"
            type="button"
            class="filter-tag"
            :class="{
              'filter-tag--active': statusFilter === opt.value,
              [`filter-tag--${opt.value || 'all'}`]: true,
            }"
            @click="statusFilter = opt.value; handleFilterChange()"
          >
            <span v-if="opt.value" class="filter-dot" :class="`filter-dot--${opt.value}`"></span>
            {{ opt.label }}
          </button>
        </div>
      </div>

      <div v-loading="loading" class="table-container">
        <el-table v-if="data && data.records.length > 0" :data="data.records" row-key="id" style="width: 100%">
          <el-table-column label="申请人" width="160" sortable>
            <template #default="{ row }">
              <strong>{{ row.username }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" sortable>
            <template #default="{ row }">
              <StatusBadge :label="statusLabelMap[row.status]" :tone="statusToneMap[row.status]" />
            </template>
          </el-table-column>
          <el-table-column label="申请时间" min-width="170" sortable>
            <template #default="{ row }">
              <span class="cell-time">
                <Clock :size="14" />
                {{ formatTime(row.createdAt) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="审核人" min-width="100">
            <template #default="{ row }">
              {{ row.reviewerName || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="审核时间" min-width="170" sortable>
            <template #default="{ row }">
              <span class="cell-time">
                <Clock :size="14" />
                {{ formatTime(row.reviewedAt) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <button class="flat-button flat-button--table-action" type="button" @click="openDialog(row)">
                查看
              </button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else-if="!loading" class="empty-state">
          <ShieldCheck :size="40" />
          <strong>暂无申请记录</strong>
        </div>
      </div>

      <div v-if="data && data.total > 0" class="pagination-wrapper">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="data.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <!-- 详情弹窗 -->
    <Transition name="modal">
      <div v-if="dialogVisible" class="modal-overlay" @click.self="closeDialog">
        <div class="modal-panel">
          <div class="modal-header">
            <h3 class="modal-title">申请详情</h3>
            <button class="modal-close" type="button" @click="closeDialog">
              <X :size="20" />
            </button>
          </div>

          <div class="modal-body" v-if="selectedApp">
            <!-- 基本信息 -->
            <div class="dialog-block dialog-block--info">
              <div class="info-left">
                <div class="info-item">
                  <span class="info-key info-key--blue">申请人</span>
                  <strong class="info-val">{{ selectedApp.username }}</strong>
                </div>
                <div class="info-item">
                  <span class="info-key info-key--blue">申请时间</span>
                  <span class="info-val">{{ formatTime(selectedApp.createdAt) }}</span>
                </div>
              </div>
              <div class="info-right">
                <StatusBadge :label="statusLabelMap[selectedApp.status]" :tone="statusToneMap[selectedApp.status]" />
              </div>
            </div>

            <!-- 申请理由 -->
            <div class="dialog-block dialog-block--reason">
              <label class="dialog-label dialog-label--blue">申请理由</label>
              <pre class="dialog-pre">{{ selectedApp.reason }}</pre>
            </div>

            <!-- 已审核：审核结果 -->
            <template v-if="selectedApp.status !== 'PENDING'">
              <div class="dialog-block dialog-block--result">
                <div class="info-left">
                  <div class="info-item">
                    <span class="info-key info-key--purple">审核人</span>
                    <strong class="info-val">{{ selectedApp.reviewerName || '-' }}</strong>
                  </div>
                  <div class="info-item">
                    <span class="info-key info-key--purple">审核时间</span>
                    <span class="info-val">{{ formatTime(selectedApp.reviewedAt) }}</span>
                  </div>
                </div>
              </div>
              <div v-if="selectedApp.reviewNote" class="dialog-block dialog-block--note">
                <label class="dialog-label dialog-label--amber">审核意见</label>
                <pre class="dialog-pre">{{ selectedApp.reviewNote }}</pre>
              </div>
            </template>

            <!-- 待审批：填写审核意见 -->
            <template v-else>
              <div class="dialog-block dialog-block--note">
                <label class="dialog-label dialog-label--amber">审核意见</label>
                <el-input
                  v-model="reviewNote"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入审核意见（可选）..."
                  maxlength="200"
                  show-word-limit
                />
              </div>
            </template>
          </div>

          <div class="modal-footer">
            <template v-if="selectedApp?.status === 'PENDING'">
              <button class="flat-button flat-button--ghost" type="button" @click="closeDialog">取消</button>
              <button
                class="flat-button flat-button--danger"
                type="button"
                :disabled="reviewing"
                @click="handleReject"
              >
                <X :size="18" />
                拒绝
              </button>
              <button
                class="flat-button flat-button--success"
                type="button"
                :disabled="reviewing"
                @click="handleApprove"
              >
                <Check :size="18" />
                批准
              </button>
            </template>
            <template v-else>
              <button class="flat-button flat-button--ghost" type="button" @click="closeDialog">关闭</button>
            </template>
          </div>
        </div>
      </div>
    </Transition>
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
  font-size: 1.6rem;
}

.list-stats {
  color: var(--fg-soft);
  font-size: 0.85rem;
  font-weight: 600;
}

.filter-bar {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
  align-items: center;
}

.filter-tags {
  display: flex;
  gap: 0;
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.5rem 0.85rem;
  border: 2px solid var(--fg);
  background: white;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
  margin-left: -2px;
}

.filter-tag:first-child {
  margin-left: 0;
}

.filter-tag:hover {
  background: var(--muted);
}

.filter-tag--active {
  background: var(--fg);
  color: white;
}

.filter-tag--active:hover {
  background: var(--fg);
}

.filter-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  flex-shrink: 0;
}

.filter-dot--PENDING {
  background: #f59e0b;
}

.filter-dot--APPROVED {
  background: #10b981;
}

.filter-dot--REJECTED {
  background: #ef4444;
}

.filter-tag--active .filter-dot--PENDING {
  background: #fbbf24;
}

.filter-tag--active .filter-dot--APPROVED {
  background: #34d399;
}

.filter-tag--active .filter-dot--REJECTED {
  background: #f87171;
}

.table-container {
  min-width: 0;
  width: 100%;
  overflow-x: auto;
}

.cell-time {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--fg-soft);
  font-size: 0.85rem;
}

.flat-button--table-action {
  min-height: 36px;
  padding: 0.35rem 0.9rem;
  font-size: 0.82rem;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 0.5rem;
  min-height: 220px;
  border: 2px dashed var(--fg);
  background: var(--muted);
  color: var(--fg-soft);
}

.empty-state strong {
  font-size: 1.1rem;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 1.25rem;
}

/* Custom Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
}

.modal-panel {
  width: 560px;
  max-width: 92vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  background: white;
  border: 2px solid var(--fg);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border-bottom: 2px solid var(--border);
  flex-shrink: 0;
}

.modal-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 2px solid var(--border);
  background: white;
  color: var(--fg-soft);
  cursor: pointer;
  transition: all 0.15s;
}

.modal-close:hover {
  background: var(--muted);
  color: var(--fg);
}

.modal-body {
  padding: 1rem;
  flex: 1;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  padding: 1rem;
  border-top: 2px solid var(--border);
  flex-shrink: 0;
}

/* Modal transition */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .modal-panel,
.modal-leave-active .modal-panel {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-panel {
  transform: translateY(-20px) scale(0.96);
  opacity: 0;
}

.modal-leave-to .modal-panel {
  transform: translateY(-20px) scale(0.96);
  opacity: 0;
}

.dialog-block {
  display: grid;
  gap: 0.6rem;
  padding: 1rem;
  border: 2px solid var(--border);
  border-left: 4px solid var(--border);
  background: white;
}

.dialog-block + .dialog-block {
  margin-top: -2px;
}

.dialog-block--info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border-left-color: #3b82f6;
  background: #eff6ff;
}

.dialog-block--reason {
  border-left-color: #3b82f6;
}

.dialog-block--result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border-left-color: #8b5cf6;
  background: #f5f3ff;
}

.dialog-block--note {
  border-left-color: #f59e0b;
  background: #fffbeb;
}

.info-left {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.info-key {
  font-size: 0.74rem;
  font-weight: 800;
  color: var(--fg-soft);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.info-key--blue {
  color: #2563eb;
}

.info-key--purple {
  color: #7c3aed;
}

.info-val {
  font-size: 0.95rem;
  font-weight: 600;
}

.info-right {
  flex-shrink: 0;
}

.dialog-label {
  font-size: 0.74rem;
  font-weight: 800;
  color: var(--fg-soft);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.dialog-label--blue {
  color: #2563eb;
}

.dialog-label--amber {
  color: #d97706;
}

.dialog-pre {
  margin: 0;
  padding: 0.85rem 1rem;
  border: 2px solid var(--border);
  background: var(--muted);
  font-family: inherit;
  font-size: 0.92rem;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: none;
  overflow-y: visible;
}


.flat-button--danger {
  background: #ef4444;
  border-color: var(--fg);
  color: white;
}

.flat-button--danger:hover:not(:disabled) {
  background: #dc2626;
  transform: scale(1.02);
}

.flat-button--success {
  background: #059669;
  border-color: var(--fg);
  color: white;
}

.flat-button--success:hover:not(:disabled) {
  background: #047857;
  transform: scale(1.02);
}

.flat-button--danger:disabled,
.flat-button--success:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 960px) {
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-tags {
    margin-left: 0;
    flex-wrap: wrap;
  }

  .filter-tag {
    flex: 1;
    justify-content: center;
    min-width: 0;
  }

  .dialog-block--info,
  .dialog-block--result {
    flex-direction: column;
    align-items: flex-start;
  }

  .modal-footer {
    flex-direction: column;
  }

  .modal-footer .flat-button {
    width: 100%;
  }
}
</style>
