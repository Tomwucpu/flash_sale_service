<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight } from 'lucide-vue-next'
import { publicActivityApi } from '@/api/public-activity'
import { ApiClientError } from '@/api/request'
import ActivityStatusBadges from '@/components/ActivityStatusBadges.vue'
import { formatDisplayDateTime } from '@/utils/date'
import { phaseLabelMap } from '@/utils/activity'
import type { ActivityPhase, ActivitySummary } from '@/types'

const loading = ref(false)
const activities = ref<ActivitySummary[]>([])
const currentPage = ref(1)
const pageSize = 9
const total = ref(0)
const errorMessage = ref('')
const activePhase = ref<ActivityPhase | ''>('')

const toneMap = ['blue', 'green', 'amber'] as const

const phaseTabs: Array<{ label: string; value: ActivityPhase | '' }> = [
  { label: '全部', value: '' },
  { label: phaseLabelMap.PREVIEW, value: 'PREVIEW' },
  { label: phaseLabelMap.ONGOING, value: 'ONGOING' },
  { label: phaseLabelMap.ENDED, value: 'ENDED' },
]

async function loadActivities(page = 1) {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await publicActivityApi.list(page, pageSize, activePhase.value || undefined)
    activities.value = result.records
    currentPage.value = result.page
    total.value = result.total
  } catch (error) {
    errorMessage.value = error instanceof ApiClientError ? error.message : '活动列表加载失败'
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  loadActivities(page)
}

function handlePhaseChange(phase: ActivityPhase | '') {
  activePhase.value = phase
}

watch(activePhase, () => loadActivities(1))

onMounted(() => loadActivities())
</script>

<template>
  <div class="page-shell">
    <div class="phase-tabs">
      <button
        v-for="tab in phaseTabs"
        :key="tab.value"
        class="phase-tab"
        :class="{ 'phase-tab--active': activePhase === tab.value }"
        @click="handlePhaseChange(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <section class="public-cards" v-loading="loading">
      <div v-if="errorMessage" class="empty-state public-empty-state">
        <strong>活动列表暂时不可用</strong>
        <p>{{ errorMessage }}</p>
      </div>
      <div v-else-if="activities.length === 0" class="empty-state public-empty-state">
        <strong>当前没有公开活动</strong>
      </div>
      <RouterLink
        v-else
        v-for="(activity, index) in activities"
        :key="activity.id"
        class="public-card"
        :class="`public-card--${toneMap[index % toneMap.length]}`"
        :to="`/activities/${activity.id}`"
      >
        <div class="public-card__head">
          <div>
            <div class="eyebrow">Activity #{{ activity.id }}</div>
            <h2>{{ activity.title }}</h2>
          </div>
          <ArrowRight :size="22" />
        </div>
        <ActivityStatusBadges :publish-status="activity.publishStatus" :phase="activity.phase" />
        <div class="meta-list">
          <div class="meta-row"><span>库存</span><strong>{{ activity.availableStock }} / {{ activity.totalStock }}</strong></div>
          <div class="meta-row"><span>开始时间</span><strong>{{ formatDisplayDateTime(activity.startTime) }}</strong></div>
          <div class="meta-row"><span>结束时间</span><strong>{{ formatDisplayDateTime(activity.endTime) }}</strong></div>
        </div>
      </RouterLink>
      <div v-if="total > pageSize" class="public-pagination">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.phase-tabs {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.phase-tab {
  padding: 0.5rem 1.25rem;
  border: 2px solid var(--fg);
  background: var(--bg);
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.phase-tab:hover {
  background: #e5e7eb;
}

.phase-tab--active {
  background: var(--fg);
  color: var(--bg);
}

.phase-tab--active:hover {
  background: var(--fg);
  color: var(--bg);
}

.public-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.public-empty-state {
  grid-column: 1 / -1;
}

.public-pagination {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  padding-top: 1rem;
}

.public-card {
  display: grid;
  gap: 1rem;
  padding: 1.25rem;
  border: 2px solid var(--fg);
  transition:
    transform 0.2s ease,
    filter 0.2s ease;
}

.public-card:hover {
  transform: scale(1.02);
  filter: saturate(1.08);
}

.public-card--blue {
  background: #dbeafe;
}

.public-card--green {
  background: #d1fae5;
}

.public-card--amber {
  background: #fef3c7;
}

.public-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.public-card__head h2 {
  margin: 0.35rem 0 0;
  font-size: 1.5rem;
}

@media (max-width: 960px) {
  .public-cards {
    grid-template-columns: 1fr;
  }
}
</style>
