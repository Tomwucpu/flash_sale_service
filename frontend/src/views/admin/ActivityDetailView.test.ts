import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ActivityDetailView from './ActivityDetailView.vue'
import type { ActivityDetail, RedeemCodeImportBatchSummary } from '@/types'

const pushSpy = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: {
      id: '1001',
    },
  }),
  useRouter: () => ({
    push: pushSpy,
  }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
  },
  ElMessageBox: {
    confirm: vi.fn(),
  },
}))

vi.mock('@/api/activity', () => ({
  activityApi: {
    detail: vi.fn(),
    publish: vi.fn(),
    offline: vi.fn(),
    importCodes: vi.fn(),
    listImportBatches: vi.fn(),
    importBatchDetail: vi.fn(),
  },
}))

const { activityApi } = await import('@/api/activity')

const ElDialogStub = defineComponent({
  name: 'ElDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
  },
  template: '<div v-if="modelValue" class="el-dialog-stub"><slot /></div>',
})

function mountView() {
  return mount(ActivityDetailView, {
    global: {
      directives: {
        loading: () => undefined,
      },
      stubs: {
        StatusBadge: true,
        Megaphone: true,
        PenSquare: true,
        SquareArrowOutUpRight: true,
        Upload: true,
        History: true,
        FileWarning: true,
        CircleAlert: true,
        'el-upload': true,
        'el-table': true,
        'el-table-column': true,
        'el-dialog': ElDialogStub,
        'el-empty': true,
        'el-scrollbar': true,
      },
    },
  })
}

function createDetail(overrides: Partial<ActivityDetail> = {}): ActivityDetail {
  return {
    id: 1001,
    title: '5元礼品卡秒杀',
    description: '限量兑换',
    coverUrl: 'https://example.com/activity.png',
    totalStock: 100,
    availableStock: 100,
    priceAmount: 0,
    needPayment: false,
    purchaseLimitType: 'SINGLE',
    purchaseLimitCount: 1,
    codeSourceMode: 'THIRD_PARTY_IMPORTED',
    publishMode: 'IMMEDIATE',
    publishStatus: 'UNPUBLISHED',
    phase: 'PREVIEW',
    publishTime: '2026-04-16T09:00:00',
    startTime: '2026-04-16T10:00:00',
    endTime: '2026-04-16T10:30:00',
    ...overrides,
  }
}

function createBatch(overrides: Partial<RedeemCodeImportBatchSummary> = {}): RedeemCodeImportBatchSummary {
  return {
    batchNo: 'IMP-1001-20260418153000001-1234',
    fileName: 'codes.csv',
    totalCount: 100,
    successCount: 96,
    failedCount: 4,
    ...overrides,
  }
}

describe('ActivityDetailView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows the import panel and loads import batches for third-party code activities', async () => {
    vi.mocked(activityApi.detail).mockResolvedValue(createDetail())
    vi.mocked(activityApi.listImportBatches).mockResolvedValue([createBatch()])

    const wrapper = mountView()
    await flushPromises()

    expect(activityApi.listImportBatches).toHaveBeenCalledWith(1001)
    expect(wrapper.text()).toContain('兑换码导入与批次记录')
    expect(wrapper.text()).toContain('codes.csv')
  })

  it('hides the import panel for system generated code activities', async () => {
    vi.mocked(activityApi.detail).mockResolvedValue(createDetail({ codeSourceMode: 'SYSTEM_GENERATED' }))
    vi.mocked(activityApi.listImportBatches).mockResolvedValue([])

    const wrapper = mountView()
    await flushPromises()

    expect(activityApi.listImportBatches).not.toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('兑换码导入与批次记录')
  })
})
