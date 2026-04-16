<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Megaphone, PenSquare, SquareArrowOutUpRight } from 'lucide-vue-next'
import { activityApi } from '@/api/activity'
import { ApiClientError } from '@/api/request'
import StatusBadge from '@/components/StatusBadge.vue'
import { getPhaseLabel, getPublishStatusLabel, isEditableActivity } from '@/utils/activity'
import { formatDisplayDateTime } from '@/utils/date'
import type { ActivityDetail } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<ActivityDetail | null>(null)

const activityId = computed(() => Number(route.params.id))

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await activityApi.detail(activityId.value)
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '活动详情加载失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

async function handlePublish() {
  await ElMessageBox.confirm('确认执行发布动作？系统将按照配置立即发布或进入定时调度。', '发布活动', {
    type: 'warning',
  })
  await activityApi.publish(activityId.value)
  ElMessage.success('发布动作已提交')
  await loadDetail()
}

async function handleOffline() {
  await ElMessageBox.confirm('确认执行下线动作？', '下线活动', {
    type: 'warning',
  })
  await activityApi.offline(activityId.value)
  ElMessage.success('活动已下线')
  await loadDetail()
}

onMounted(loadDetail)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="page-header page-header--amber" v-if="detail">
      <div class="eyebrow">Activity Detail</div>
      <h1 class="poster-title">{{ detail.title }}</h1>
      <p class="poster-copy">{{ detail.description || '当前活动没有填写补充描述。' }}</p>
      <div class="badge-stack">
        <StatusBadge
          :label="getPublishStatusLabel(detail.publishStatus)"
          :tone="detail.publishStatus === 'PUBLISHED' ? 'green' : detail.publishStatus === 'UNPUBLISHED' ? 'amber' : 'slate'"
        />
        <StatusBadge
          :label="getPhaseLabel(detail.phase)"
          :tone="detail.phase === 'ONGOING' ? 'blue' : detail.phase === 'PREVIEW' ? 'amber' : 'slate'"
        />
      </div>
    </section>

    <section class="detail-grid" v-if="detail">
      <article class="flat-panel">
        <div class="eyebrow">Overview</div>
        <div class="meta-list">
          <div class="meta-row"><span>活动 ID</span><strong>#{{ detail.id }}</strong></div>
          <div class="meta-row"><span>封面图</span><strong>{{ detail.coverUrl || '未设置' }}</strong></div>
          <div class="meta-row"><span>库存</span><strong>{{ detail.availableStock }} / {{ detail.totalStock }}</strong></div>
          <div class="meta-row"><span>活动金额</span><strong>{{ detail.priceAmount }}</strong></div>
          <div class="meta-row"><span>支付模式</span><strong>{{ detail.needPayment ? '需要支付' : '免支付' }}</strong></div>
        </div>
      </article>

      <article class="flat-panel flat-panel--soft">
        <div class="eyebrow">Schedule</div>
        <div class="meta-list">
          <div class="meta-row"><span>限购方式</span><strong>{{ detail.purchaseLimitType }} / {{ detail.purchaseLimitCount }}</strong></div>
          <div class="meta-row"><span>兑换码来源</span><strong>{{ detail.codeSourceMode }}</strong></div>
          <div class="meta-row"><span>发布模式</span><strong>{{ detail.publishMode }}</strong></div>
          <div class="meta-row"><span>发布时间</span><strong>{{ formatDisplayDateTime(detail.publishTime) }}</strong></div>
          <div class="meta-row"><span>活动开始</span><strong>{{ formatDisplayDateTime(detail.startTime) }}</strong></div>
          <div class="meta-row"><span>活动结束</span><strong>{{ formatDisplayDateTime(detail.endTime) }}</strong></div>
        </div>
      </article>
    </section>

    <section class="detail-actions" v-if="detail">
      <button class="flat-button flat-button--secondary" type="button" :disabled="!isEditableActivity(detail)" @click="router.push(`/admin/activities/${detail.id}/edit`)">
        <PenSquare :size="18" />
        编辑活动
      </button>
      <button class="flat-button" type="button" :disabled="detail.publishStatus !== 'UNPUBLISHED'" @click="handlePublish">
        <Megaphone :size="18" />
        发布活动
      </button>
      <button class="flat-button flat-button--ghost" type="button" :disabled="detail.publishStatus === 'OFFLINE'" @click="handleOffline">
        <SquareArrowOutUpRight :size="18" />
        下线活动
      </button>
    </section>
  </div>
</template>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.badge-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

@media (max-width: 960px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
