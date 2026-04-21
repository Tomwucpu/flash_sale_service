<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, CalendarClock, Save, TicketPercent } from 'lucide-vue-next'
import { activityApi } from '@/api/activity'
import { ApiClientError } from '@/api/request'
import { codeSourceOptions, isEditableActivity, publishModeOptions, purchaseLimitOptions } from '@/utils/activity'
import { parseApiDateTime } from '@/utils/date'
import { toActivityPayload } from '@/utils/activity-form'
import type { ActivityDetail, ActivityFormModel } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const detail = ref<ActivityDetail | null>(null)

const isEditing = computed(() => Boolean(route.params.id))
const activityId = computed(() => Number(route.params.id))
const canSubmit = computed(() => !isEditing.value || (detail.value ? isEditableActivity(detail.value) : true))

function createDefaultForm(): ActivityFormModel {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours() + 1, 0, 0)
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours() + 2, 0, 0)

  return {
    title: '',
    description: '',
    coverUrl: '',
    totalStock: 100,
    priceAmount: 0,
    needPayment: false,
    purchaseLimitType: 'SINGLE',
    purchaseLimitCount: 1,
    codeSourceMode: 'SYSTEM_GENERATED',
    publishMode: 'IMMEDIATE',
    publishTime: null,
    startTime: start,
    endTime: end,
  }
}

const form = reactive<ActivityFormModel>(createDefaultForm())

watch(
  () => form.needPayment,
  (needPayment) => {
    if (!needPayment) {
      form.priceAmount = 0
    }
  },
)

watch(
  () => form.purchaseLimitType,
  (purchaseLimitType) => {
    if (purchaseLimitType === 'SINGLE') {
      form.purchaseLimitCount = 1
    }
  },
  { immediate: true },
)

watch(
  () => form.publishMode,
  (publishMode) => {
    if (publishMode === 'IMMEDIATE') {
      form.publishTime = null
      return
    }
    if (!form.publishTime) {
      form.publishTime = form.startTime
    }
  },
  { immediate: true },
)

function fillForm(detailResponse: ActivityDetail) {
  form.title = detailResponse.title
  form.description = detailResponse.description
  form.coverUrl = detailResponse.coverUrl
  form.totalStock = detailResponse.totalStock
  form.priceAmount = detailResponse.priceAmount
  form.needPayment = detailResponse.needPayment
  form.purchaseLimitType = detailResponse.purchaseLimitType
  form.purchaseLimitCount = detailResponse.purchaseLimitCount
  form.codeSourceMode = detailResponse.codeSourceMode
  form.publishMode = detailResponse.publishMode
  form.publishTime = parseApiDateTime(detailResponse.publishTime)
  form.startTime = parseApiDateTime(detailResponse.startTime)
  form.endTime = parseApiDateTime(detailResponse.endTime)
}

async function loadDetail() {
  if (!isEditing.value) {
    return
  }

  loading.value = true
  try {
    detail.value = await activityApi.detail(activityId.value)
    fillForm(detail.value)
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '活动详情加载失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (form.publishMode === 'SCHEDULED' && !form.publishTime) {
    ElMessage.error('定时发布需要设置发布时间')
    return
  }

  submitting.value = true
  try {
    const payload = toActivityPayload(form)
    const response = isEditing.value
      ? await activityApi.update(activityId.value, payload)
      : await activityApi.create(payload)

    ElMessage.success(isEditing.value ? '活动已更新' : '活动已创建')
    await router.push(`/admin/activities/${response.id}`)
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '保存活动失败'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="page-shell">
    <section class="page-header page-header--green">
      <div class="eyebrow">{{ isEditing ? 'Activity Edit' : 'Activity Create' }}</div>
      <h1 class="poster-title">{{ isEditing ? '编辑活动' : '新建活动' }}</h1>
    </section>

    <section v-if="isEditing && detail && !canSubmit" class="flat-panel flat-panel--amber">
      当前活动状态为 {{ detail.publishStatus }}，不允许编辑。
    </section>

    <section class="editor-grid" v-loading="loading">
      <article class="flat-panel">
        <div class="editor-section-title">
          <TicketPercent :size="22" />
          <div>
            <div class="eyebrow">Content</div>
            <h2>活动基础信息</h2>
          </div>
        </div>
        <el-form label-position="top">
          <div class="flat-grid flat-grid--2">
            <el-form-item label="活动标题">
              <el-input v-model="form.title" maxlength="128" placeholder="例如：5元礼品卡秒杀" />
            </el-form-item>
            <el-form-item label="封面图地址">
              <el-input v-model="form.coverUrl" maxlength="255" placeholder="https://example.com/activity.png" />
            </el-form-item>
          </div>
          <el-form-item label="活动描述">
            <el-input v-model="form.description" type="textarea" :rows="4" maxlength="2000" />
          </el-form-item>
          <div class="flat-grid" :class="{ 'flat-grid--2': form.needPayment }">
            <el-form-item label="活动库存">
              <el-input-number v-model="form.totalStock" :min="1" :step="10" />
            </el-form-item>
            <el-form-item v-if="form.needPayment" label="活动金额">
              <el-input-number v-model="form.priceAmount" :min="0" :step="1" :precision="2" />
            </el-form-item>
          </div>
          <el-form-item label="是否需要支付">
            <el-switch v-model="form.needPayment" inline-prompt active-text="支付" inactive-text="免付" />
          </el-form-item>
        </el-form>
      </article>

      <article class="flat-panel flat-panel--soft">
        <div class="editor-section-title">
          <CalendarClock :size="22" />
          <div>
            <div class="eyebrow">Rules</div>
            <h2>规则与发布节奏</h2>
          </div>
        </div>
        <el-form label-position="top">
          <div class="flat-grid flat-grid--2">
            <el-form-item label="限购类型">
              <el-segmented v-model="form.purchaseLimitType" :options="purchaseLimitOptions" block />
            </el-form-item>
            <el-form-item label="限购次数">
              <el-input-number v-model="form.purchaseLimitCount" :min="1" :disabled="form.purchaseLimitType === 'SINGLE'" />
            </el-form-item>
          </div>
          <div class="flat-grid flat-grid--2">
            <el-form-item label="兑换码来源">
              <el-radio-group v-model="form.codeSourceMode" class="radio-stack">
                <el-radio-button v-for="item in codeSourceOptions" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="发布模式">
              <el-radio-group v-model="form.publishMode" class="radio-stack">
                <el-radio-button v-for="item in publishModeOptions" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </div>
          <div class="flat-grid" :class="{ 'flat-grid--2': form.publishMode === 'SCHEDULED' }">
            <el-form-item v-if="form.publishMode === 'SCHEDULED'" label="发布时间">
              <el-date-picker v-model="form.publishTime" type="datetime" placeholder="选择发布时间" />
            </el-form-item>
            <el-form-item label="活动开始时间">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" />
            </el-form-item>
          </div>
          <el-form-item label="活动结束时间">
            <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" />
          </el-form-item>
        </el-form>
      </article>
    </section>

    <section class="editor-actions">
      <button class="flat-button flat-button--ghost" type="button" @click="router.back()">
        <ArrowLeft :size="18" />
        返回上一页
      </button>
      <button class="flat-button" type="button" :disabled="submitting || !canSubmit" @click="handleSubmit">
        <Save :size="18" />
        {{ submitting ? '保存中...' : isEditing ? '保存修改' : '创建活动' }}
        <ArrowRight :size="18" />
      </button>
    </section>
  </div>
</template>

<style scoped>
.editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.editor-section-title {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  margin-bottom: 1rem;
}

.editor-section-title h2 {
  margin: 0.25rem 0 0;
  font-size: 1.35rem;
}

.radio-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.editor-actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

@media (max-width: 960px) {
  .editor-grid,
  .editor-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
