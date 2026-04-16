<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { BanknoteArrowUp, ShoppingBag, Ticket } from 'lucide-vue-next'
import { publicActivityApi } from '@/api/public-activity'
import StatusBadge from '@/components/StatusBadge.vue'
import { formatDisplayDateTime } from '@/utils/date'
import { getPhaseLabel, getPublishStatusLabel } from '@/utils/activity'
import type { ActivityDetail } from '@/types'

const route = useRoute()
const detail = ref<ActivityDetail | null>(null)
const activityId = computed(() => Number(route.params.id))

onMounted(async () => {
  detail.value = await publicActivityApi.detail(activityId.value)
})
</script>

<template>
  <div class="page-shell" v-if="detail">
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
        <div class="detail-hero__cta">
          <button class="flat-button" type="button">
            <Ticket :size="18" />
            立即抢购
          </button>
          <button class="flat-button flat-button--secondary" type="button">
            <ShoppingBag :size="18" />
            查看订单
          </button>
          <button class="flat-button flat-button--ghost" type="button">
            <BanknoteArrowUp :size="18" />
            支付占位
          </button>
        </div>
        <p class="detail-note">以上 CTA 当前为展示占位，待后端开放秒杀、订单、支付接口后接入真实流程。</p>
      </div>
    </section>

    <section class="detail-grid">
      <article class="flat-panel">
        <div class="eyebrow">Offer</div>
        <div class="meta-list">
          <div class="meta-row"><span>价格</span><strong>{{ detail.priceAmount }}</strong></div>
          <div class="meta-row"><span>库存</span><strong>{{ detail.availableStock }} / {{ detail.totalStock }}</strong></div>
          <div class="meta-row"><span>支付要求</span><strong>{{ detail.needPayment ? '需要支付' : '免支付' }}</strong></div>
          <div class="meta-row"><span>兑换码来源</span><strong>{{ detail.codeSourceMode }}</strong></div>
        </div>
      </article>
      <article class="flat-panel flat-panel--soft">
        <div class="eyebrow">Timing</div>
        <div class="meta-list">
          <div class="meta-row"><span>发布时间</span><strong>{{ formatDisplayDateTime(detail.publishTime) }}</strong></div>
          <div class="meta-row"><span>开始时间</span><strong>{{ formatDisplayDateTime(detail.startTime) }}</strong></div>
          <div class="meta-row"><span>结束时间</span><strong>{{ formatDisplayDateTime(detail.endTime) }}</strong></div>
          <div class="meta-row"><span>限购规则</span><strong>{{ detail.purchaseLimitType }} / {{ detail.purchaseLimitCount }}</strong></div>
        </div>
      </article>
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

.detail-hero__badges,
.detail-hero__cta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.detail-note {
  margin: 0;
  color: var(--fg-soft);
  line-height: 1.6;
}

@media (max-width: 960px) {
  .detail-hero,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
