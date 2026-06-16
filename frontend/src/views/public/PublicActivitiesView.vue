<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight } from 'lucide-vue-next'
import { publicActivityApi } from '@/api/public-activity'
import { ApiClientError } from '@/api/request'
import ActivityStatusBadges from '@/components/ActivityStatusBadges.vue'
import { formatDisplayDateTime } from '@/utils/date'
import type { ActivitySummary } from '@/types'

const loading = ref(false)
const activities = ref<ActivitySummary[]>([])
const errorMessage = ref('')

const toneMap = ['blue', 'green', 'amber'] as const

onMounted(async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    activities.value = await publicActivityApi.list()
  } catch (error) {
    errorMessage.value = error instanceof ApiClientError ? error.message : '活动列表加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-shell">
    <section class="page-header page-header--green">
      <div class="eyebrow">Public Activities</div>
      <h1 class="poster-title">公开活动列表</h1>
    </section>

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
    </section>
  </div>
</template>

<style scoped>
.public-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.public-empty-state {
  grid-column: 1 / -1;
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
