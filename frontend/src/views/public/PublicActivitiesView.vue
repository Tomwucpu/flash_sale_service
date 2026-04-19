<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight } from 'lucide-vue-next'
import { publicActivityApi } from '@/api/public-activity'
import { ApiClientError } from '@/api/request'
import StatusBadge from '@/components/StatusBadge.vue'
import { formatDisplayDateTime } from '@/utils/date'
import { getPhaseLabel, getPublishStatusLabel } from '@/utils/activity'
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
      <h1 class="poster-title">用户侧先展示节奏，再接真实交易链路。</h1>
      <p class="poster-copy">
        这里直接读取后端已发布活动数据，当前会展示公开可见的预告中、进行中和已结束活动；秒杀、订单、支付链路仍等后续接口开放后再接入。
      </p>
    </section>

    <section class="public-cards" v-loading="loading">
      <div v-if="errorMessage" class="empty-state public-empty-state">
        <strong>活动列表暂时不可用</strong>
        <p>{{ errorMessage }}</p>
      </div>
      <div v-else-if="activities.length === 0" class="empty-state public-empty-state">
        <strong>当前没有公开活动</strong>
        <p>后端暂无已发布活动，等活动发布后会自动出现在这里。</p>
      </div>
      <RouterLink
        v-else
        v-for="(activity, index) in activities"
        :key="activity.id"
        class="public-card"
        :class="`public-card--${toneMap[index % toneMap.length]}`"
        :to="`/public/activities/${activity.id}`"
      >
        <div class="public-card__head">
          <div>
            <div class="eyebrow">Activity #{{ activity.id }}</div>
            <h2>{{ activity.title }}</h2>
          </div>
          <ArrowRight :size="22" />
        </div>
        <div class="public-card__badges">
          <StatusBadge
            :label="getPublishStatusLabel(activity.publishStatus)"
            :tone="activity.publishStatus === 'PUBLISHED' ? 'green' : activity.publishStatus === 'UNPUBLISHED' ? 'amber' : 'slate'"
          />
          <StatusBadge
            :label="getPhaseLabel(activity.phase)"
            :tone="activity.phase === 'ONGOING' ? 'blue' : activity.phase === 'PREVIEW' ? 'amber' : 'slate'"
          />
        </div>
        <div class="meta-list">
          <div class="meta-row"><span>库存</span><strong>{{ activity.availableStock }} / {{ activity.totalStock }}</strong></div>
          <div class="meta-row"><span>发布时间</span><strong>{{ formatDisplayDateTime(activity.publishTime) }}</strong></div>
          <div class="meta-row"><span>活动时间</span><strong>{{ formatDisplayDateTime(activity.startTime) }}</strong></div>
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

.public-card__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

@media (max-width: 960px) {
  .public-cards {
    grid-template-columns: 1fr;
  }
}
</style>
