<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { publicActivityApi } from '@/api/public-activity'
import { ApiClientError } from '@/api/request'
import StatusBadge from '@/components/StatusBadge.vue'
import { formatDisplayDateTime } from '@/utils/date'
import { getCodeSourceModeLabel, getPhaseLabel, getPublishStatusLabel } from '@/utils/activity'
import type { ActivityDetail } from '@/types'

const route = useRoute()
const detail = ref<ActivityDetail | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const activityId = computed(() => Number(route.params.id))

onMounted(async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    detail.value = await publicActivityApi.detail(activityId.value)
  } catch (error) {
    detail.value = null
    errorMessage.value = error instanceof ApiClientError ? error.message : '活动详情加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-shell" v-loading="loading" v-if="detail">
    <section class="detail-hero">
      <div class="detail-hero__image">
        <img :src="detail.coverUrl" :alt="detail.title" />
      </div>
      <div class="detail-hero__content">
        <div class="eyebrow">Public Detail</div>
        <h1 class="poster-title">{{ detail.title }}</h1>
        <p class="poster-copy">{{ detail.description }}</p>
        <div class="detail-hero__badges">
          <StatusBadge
            :label="getPublishStatusLabel(detail.publishStatus)"
            :tone="detail.publishStatus === 'PUBLISHED' ? 'green' : detail.publishStatus === 'UNPUBLISHED' ? 'amber' : 'slate'"
          />
          <StatusBadge
            :label="getPhaseLabel(detail.phase)"
            :tone="detail.phase === 'ONGOING' ? 'blue' : detail.phase === 'PREVIEW' ? 'amber' : 'slate'"
          />
        </div>
      </div>
    </section>

    <section class="detail-grid">
      <article class="flat-panel">
        <div class="eyebrow">Offer</div>
        <div class="meta-list">
          <div class="meta-row"><span>价格</span><strong>{{ detail.priceAmount }}</strong></div>
          <div class="meta-row"><span>库存</span><strong>{{ detail.availableStock }} / {{ detail.totalStock }}</strong></div>
          <div class="meta-row"><span>支付要求</span><strong>{{ detail.needPayment ? '需要支付' : '免支付' }}</strong></div>
          <div class="meta-row"><span>兑换码来源</span><strong>{{ getCodeSourceModeLabel(detail.codeSourceMode) }}</strong></div>
        </div>
      </article>
      <article class="flat-panel flat-panel--soft">
        <div class="eyebrow">Timing</div>
        <div class="meta-list">
          <div class="meta-row"><span>发布时间</span><strong>{{ formatDisplayDateTime(detail.publishTime) }}</strong></div>
          <div class="meta-row"><span>开始时间</span><strong>{{ formatDisplayDateTime(detail.startTime) }}</strong></div>
          <div class="meta-row"><span>结束时间</span><strong>{{ formatDisplayDateTime(detail.endTime) }}</strong></div>
          <div class="meta-row"><span>限购规则</span><strong>{{ detail.purchaseLimitType === 'SINGLE' ? '单人一次' : '单人多次' }} / {{ detail.purchaseLimitCount }}</strong></div>
        </div>
      </article>
    </section>
  </div>
  <div class="page-shell" v-else>
    <section class="flat-panel flat-panel--amber">
      <div class="eyebrow">Public Detail</div>
      <h1 class="poster-title">活动不存在</h1>
      <p class="poster-copy">{{ errorMessage || '你访问的活动不存在。' }}</p>
    </section>
  </div>
</template>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 1rem;
}

.detail-hero__image,
.detail-hero__content {
  border: 2px solid var(--fg);
}

.detail-hero__image img {
  width: 100%;
  height: 100%;
  min-height: 420px;
  object-fit: cover;
  display: block;
}

.detail-hero__content {
  display: grid;
  align-content: center;
  gap: 1rem;
  padding: 2rem;
  background: #dbeafe;
}

.detail-hero__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

@media (max-width: 960px) {
  .detail-hero,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
