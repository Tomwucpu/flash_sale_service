<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Eye, Megaphone, PenSquare, Plus, ReceiptText, SquareArrowOutUpRight, Trash2 } from 'lucide-vue-next'
import { activityApi } from '@/api/activity'
import { ApiClientError } from '@/api/request'
import StatusBadge from '@/components/StatusBadge.vue'
import { formatDisplayDateTime } from '@/utils/date'
import { getPhaseLabel, getPublishModeLabel, getPublishStatusLabel, isDeletableActivity, isEditableActivity } from '@/utils/activity'
import type { ActivitySummary } from '@/types'

const router = useRouter()
const loading = ref(false)
const activities = ref<ActivitySummary[]>([])

const summary = computed(() => ({
  total: activities.value.length,
  draft: activities.value.filter((item) => item.publishStatus === 'UNPUBLISHED').length,
  live: activities.value.filter((item) => item.publishStatus === 'PUBLISHED').length,
}))
const hasActivities = computed(() => activities.value.length > 0)

async function loadActivities() {
  loading.value = true
  try {
    activities.value = await activityApi.list()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '活动列表加载失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

async function handlePublish(activity: ActivitySummary) {
  const isAdvancePublish = activity.publishMode === 'SCHEDULED'
  await ElMessageBox.confirm(isAdvancePublish ? '确认提前发布该定时活动？' : '确认立即发布活动？', isAdvancePublish ? '提前发布' : '发布活动', {
    type: 'warning',
    confirmButtonText: isAdvancePublish ? '确认提前发布' : '确认发布',
  })
  if (isAdvancePublish) {
    await activityApi.advancePublish(activity.id)
  } else {
    await activityApi.publish(activity.id)
  }
  ElMessage.success(isAdvancePublish ? '活动已提前发布' : '活动已立即发布')
  await loadActivities()
}

async function handleOffline(activityId: number) {
  await ElMessageBox.confirm('确认下线活动？', '下线活动', {
    type: 'warning',
    confirmButtonText: '确认下线',
  })
  await activityApi.offline(activityId)
  ElMessage.success('活动已下线')
  await loadActivities()
}

async function handleDelete(activityId: number) {
  await ElMessageBox.confirm('确认删除活动？', '删除活动', {
    type: 'warning',
    confirmButtonText: '确认删除',
  })
  await activityApi.delete(activityId)
  ElMessage.success('活动已删除')
  await loadActivities()
}

onMounted(loadActivities)
</script>

<template>
  <div class="page-shell">
    <section class="page-header page-header--blue">
      <div class="eyebrow">Activity Console</div>
      <h1 class="poster-title">活动管理</h1>
      <div class="stat-grid">
        <article class="stat-block">
          <strong>{{ summary.total }}</strong>
          <span>活动总数</span>
        </article>
        <article class="stat-block" style="background: #fef3c7">
          <strong>{{ summary.draft }}</strong>
          <span>未发布</span>
        </article>
        <article class="stat-block" style="background: #d1fae5">
          <strong>{{ summary.live }}</strong>
          <span>已发布</span>
        </article>
      </div>
    </section>

    <section class="flat-panel">
      <div class="list-toolbar">
        <div>
          <div class="eyebrow">Manage</div>
          <h2>活动列表</h2>
        </div>
        <button class="flat-button" type="button" @click="router.push('/admin/activities/create')">
          <Plus :size="18" />
          新建活动
        </button>
      </div>

      <div v-loading="loading">
        <el-table v-if="hasActivities" :data="activities" row-key="id">
          <el-table-column prop="title" label="活动信息" min-width="220">
            <template #default="{ row }">
              <div class="cell-stack">
                <strong>{{ row.title }}</strong>
                <span>ID #{{ row.id }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="库存" width="140">
            <template #default="{ row }">
              <div class="cell-stack">
                <strong>{{ row.availableStock }} / {{ row.totalStock }}</strong>
                <span>可用 / 总量</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="发布配置" min-width="220">
            <template #default="{ row }">
              <div class="cell-stack">
                <strong>{{ getPublishModeLabel(row.publishMode) }}</strong>
                <span>发布时间 {{ formatDisplayDateTime(row.publishTime) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="220">
            <template #default="{ row }">
              <div class="badge-stack">
                <StatusBadge
                  :label="getPublishStatusLabel(row.publishStatus)"
                  :tone="row.publishStatus === 'PUBLISHED' ? 'green' : row.publishStatus === 'OFFLINE' ? 'slate' : 'amber'"
                />
                <StatusBadge
                  :label="getPhaseLabel(row.phase)"
                  :tone="row.phase === 'ONGOING' ? 'blue' : row.phase === 'PREVIEW' ? 'amber' : 'slate'"
                />
              </div>
            </template>
          </el-table-column>
          <el-table-column label="时间窗" min-width="210">
            <template #default="{ row }">
              <div class="cell-stack">
                <strong>{{ formatDisplayDateTime(row.startTime) }}</strong>
                <span>至 {{ formatDisplayDateTime(row.endTime) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="320" fixed="right">
            <template #default="{ row }">
              <div class="action-cell">
                <button class="flat-button flat-button--ghost action-button" type="button" @click="router.push(`/admin/activities/${row.id}`)">
                  <Eye :size="16" />
                  查看
                </button>
                <button class="flat-button flat-button--ghost action-button" type="button" @click="router.push(`/admin/activities/${row.id}/orders`)">
                  <ReceiptText :size="16" />
                  订单
                </button>
                <button
                  v-if="isEditableActivity(row)"
                  class="flat-button flat-button--secondary action-button"
                  type="button"
                  @click="router.push(`/admin/activities/${row.id}/edit`)"
                >
                  <PenSquare :size="16" />
                  编辑
                </button>
                <button
                  v-if="row.publishStatus === 'UNPUBLISHED'"
                  class="flat-button action-button"
                  type="button"
                  @click="handlePublish(row)"
                >
                  <Megaphone :size="16" />
                  {{ row.publishMode === 'SCHEDULED' ? '提前发布' : '立即发布' }}
                </button>
                <button
                  v-if="row.publishStatus !== 'OFFLINE'"
                  class="flat-button flat-button--ghost action-button"
                  type="button"
                  @click="handleOffline(row.id)"
                >
                  <SquareArrowOutUpRight :size="16" />
                  下线
                </button>
                <button
                  class="flat-button flat-button--ghost action-button"
                  type="button"
                  :disabled="!isDeletableActivity(row)"
                  @click="handleDelete(row.id)"
                >
                  <Trash2 :size="16" />
                  删除
                </button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state" data-testid="activity-empty-state">
          <strong>当前还没有活动</strong>
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
  font-size: 1.6rem;
}

.cell-stack,
.action-cell {
  display: grid;
  gap: 0.35rem;
}

.cell-stack strong {
  font-size: 0.98rem;
}

.cell-stack span {
  color: var(--fg-soft);
  font-size: 0.8rem;
  line-height: 1.4;
}

.badge-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.action-cell {
  align-items: start;
}

.action-button {
  min-height: 42px;
  padding: 0.55rem 0.8rem;
}

.empty-state {
  display: grid;
  justify-items: start;
  gap: 0.9rem;
  padding: 1.5rem;
  border: 2px dashed var(--border);
  background: var(--muted);
}

.empty-state strong {
  font-size: 1.1rem;
}

@media (max-width: 960px) {
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
