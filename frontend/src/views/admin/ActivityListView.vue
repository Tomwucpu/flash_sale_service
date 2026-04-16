<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Eye, Megaphone, PenSquare, Plus, SquareArrowOutUpRight } from 'lucide-vue-next'
import { activityApi } from '@/api/activity'
import { ApiClientError } from '@/api/request'
import StatusBadge from '@/components/StatusBadge.vue'
import { formatDisplayDateTime } from '@/utils/date'
import { getPhaseLabel, getPublishStatusLabel, isEditableActivity } from '@/utils/activity'
import type { ActivitySummary } from '@/types'

const router = useRouter()
const loading = ref(false)
const activities = ref<ActivitySummary[]>([])

const summary = computed(() => ({
  total: activities.value.length,
  draft: activities.value.filter((item) => item.publishStatus === 'UNPUBLISHED').length,
  live: activities.value.filter((item) => item.publishStatus === 'PUBLISHED').length,
}))

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

async function handlePublish(activityId: number) {
  await ElMessageBox.confirm('发布后将按配置立即上线或进入定时调度，继续吗？', '发布活动', {
    type: 'warning',
    confirmButtonText: '确认发布',
  })
  await activityApi.publish(activityId)
  ElMessage.success('发布动作已提交')
  await loadActivities()
}

async function handleOffline(activityId: number) {
  await ElMessageBox.confirm('下线会同步移除活动缓存和可见索引，继续吗？', '下线活动', {
    type: 'warning',
    confirmButtonText: '确认下线',
  })
  await activityApi.offline(activityId)
  ElMessage.success('活动已下线')
  await loadActivities()
}

onMounted(loadActivities)
</script>

<template>
  <div class="page-shell">
    <section class="page-header page-header--blue">
      <div class="eyebrow">Activity Console</div>
      <h1 class="poster-title">发布计划不需要被埋进表格深处。</h1>
      <p class="poster-copy">
        后台列表直接对接后端活动接口。每条记录都保留发布状态、阶段、时间窗和动作入口，便于联调角色与发布节奏一起验证。
      </p>
      <div class="stat-grid">
        <article class="stat-block">
          <strong>{{ summary.total }}</strong>
          <span>当前活动总数</span>
        </article>
        <article class="stat-block" style="background: #fef3c7">
          <strong>{{ summary.draft }}</strong>
          <span>未发布，可编辑</span>
        </article>
        <article class="stat-block" style="background: #d1fae5">
          <strong>{{ summary.live }}</strong>
          <span>已发布，联调中</span>
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

      <el-table v-loading="loading" :data="activities" row-key="id">
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
              <strong>{{ row.publishMode }}</strong>
              <span>{{ formatDisplayDateTime(row.publishTime) }}</span>
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
        <el-table-column label="操作" min-width="280" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <button class="flat-button flat-button--ghost action-button" type="button" @click="router.push(`/admin/activities/${row.id}`)">
                <Eye :size="16" />
                查看
              </button>
              <button
                class="flat-button flat-button--secondary action-button"
                type="button"
                :disabled="!isEditableActivity(row)"
                @click="router.push(`/admin/activities/${row.id}/edit`)"
              >
                <PenSquare :size="16" />
                编辑
              </button>
              <button
                class="flat-button action-button"
                type="button"
                :disabled="row.publishStatus !== 'UNPUBLISHED'"
                @click="handlePublish(row.id)"
              >
                <Megaphone :size="16" />
                发布
              </button>
              <button
                class="flat-button flat-button--ghost action-button"
                type="button"
                :disabled="row.publishStatus === 'OFFLINE'"
                @click="handleOffline(row.id)"
              >
                <SquareArrowOutUpRight :size="16" />
                下线
              </button>
              <span class="action-hint">
                {{ isEditableActivity(row) ? '未发布状态可编辑' : '已发布或已下线，仅支持查看和下线' }}
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>
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

.cell-stack span,
.action-hint {
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

@media (max-width: 960px) {
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
