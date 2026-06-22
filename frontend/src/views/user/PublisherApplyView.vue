<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, Check, Clock, Send, X } from 'lucide-vue-next'
import { applicationApi } from '@/api/application'
import { ApiClientError } from '@/api/request'
import type { PublisherApplication } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const reason = ref('')
const application = ref<PublisherApplication | null>(null)

const statusMap: Record<string, { label: string; color: string; icon: typeof Clock }> = {
  PENDING: { label: '待审批', color: '#f59e0b', icon: Clock },
  APPROVED: { label: '已通过', color: '#10b981', icon: Check },
  REJECTED: { label: '已拒绝', color: '#ef4444', icon: X },
}

async function loadApplication() {
  loading.value = true
  try {
    application.value = await applicationApi.getMyApplication()
  } catch {
    // 没有申请记录是正常的
    application.value = null
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (reason.value.trim().length === 0) {
    ElMessage.warning('请填写申请理由')
    return
  }
  if (reason.value.trim().length > 500) {
    ElMessage.warning('申请理由最多 500 字')
    return
  }

  submitting.value = true
  try {
    application.value = await applicationApi.apply({ reason: reason.value.trim() })
    ElMessage.success('申请已提交，请等待管理员审核')
    reason.value = ''
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '提交失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

onMounted(loadApplication)
</script>

<template>
  <div class="apply-page">
    <div class="page-header page-header--blue">
      <div class="eyebrow">Publisher Application</div>
      <h2 class="apply-page__heading">申请成为发布者</h2>
      <p class="poster-copy">成为发布者后可以创建和管理秒杀活动。</p>
    </div>

    <div v-if="loading" class="flat-panel apply-page__loading">加载中...</div>

    <template v-else>
      <!-- 已有申请记录 -->
      <div v-if="application" class="flat-panel apply-page__status-card">
        <div class="apply-page__status-header">
          <h3>申请状态</h3>
          <span
            class="apply-page__status-badge"
            :style="{ color: statusMap[application.status]?.color }"
          >
            <component :is="statusMap[application.status]?.icon" :size="16" />
            {{ statusMap[application.status]?.label }}
          </span>
        </div>

        <div class="apply-page__field">
          <label>申请理由</label>
          <p>{{ application.reason }}</p>
        </div>

        <div v-if="application.reviewNote" class="apply-page__field">
          <label>审核意见</label>
          <p>{{ application.reviewNote }}</p>
        </div>

        <div v-if="application.reviewedAt" class="apply-page__field">
          <label>审核时间</label>
          <p>{{ new Date(application.reviewedAt).toLocaleString() }}</p>
        </div>

        <!-- 被拒绝后可以重新申请 -->
        <div v-if="application.status === 'REJECTED'" class="apply-page__reapply">
          <el-divider />
          <h3>重新申请</h3>
          <el-input
            v-model="reason"
            type="textarea"
            :rows="4"
            placeholder="请补充申请理由..."
            maxlength="500"
            show-word-limit
          />
          <button
            class="flat-button apply-page__submit"
            :disabled="submitting"
            @click="handleSubmit"
          >
            <Send :size="18" />
            {{ submitting ? '提交中...' : '重新提交申请' }}
            <ArrowRight :size="18" />
          </button>
        </div>
      </div>

      <!-- 无申请记录，显示申请表单 -->
      <div v-else class="flat-panel apply-page__form">
        <p class="apply-page__hint">
          提交申请后，管理员将审核您的申请。审核通过后您的账号将升级为发布者角色。
        </p>
        <el-input
          v-model="reason"
          type="textarea"
          :rows="6"
          placeholder="请说明您申请成为发布者的理由..."
          maxlength="500"
          show-word-limit
        />
        <button
          class="flat-button apply-page__submit"
          :disabled="submitting"
          @click="handleSubmit"
        >
          <Send :size="18" />
          {{ submitting ? '提交中...' : '提交申请' }}
          <ArrowRight :size="18" />
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.apply-page {
  display: grid;
  gap: 1.25rem;
  max-width: 640px;
}

.apply-page__heading {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 800;
}

.apply-page__loading {
  padding: 2rem;
  text-align: center;
  color: var(--fg-soft);
}

.apply-page__status-card {
  display: grid;
  gap: 1rem;
  padding: 1.5rem;
}

.apply-page__status-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.apply-page__status-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
}

.apply-page__status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-weight: 700;
  font-size: 0.95rem;
}

.apply-page__field {
  display: grid;
  gap: 0.25rem;
}

.apply-page__field label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--fg-soft);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.apply-page__field p {
  margin: 0;
  line-height: 1.6;
}

.apply-page__reapply {
  display: grid;
  gap: 0.75rem;
}

.apply-page__reapply h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}

.apply-page__form {
  display: grid;
  gap: 1rem;
  padding: 1.5rem;
}

.apply-page__hint {
  margin: 0;
  color: var(--fg-soft);
  line-height: 1.6;
}

.apply-page__submit {
  width: 100%;
}
</style>
