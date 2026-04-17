import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import ActivityListView from './ActivityListView.vue'
import type { ActivitySummary } from '@/types'

const pushSpy = vi.fn()

vi.mock('vue-router', () => ({
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
    list: vi.fn(),
    publish: vi.fn(),
    offline: vi.fn(),
    delete: vi.fn(),
  },
}))

const { activityApi } = await import('@/api/activity')

const ElTableStub = defineComponent({
  name: 'ElTable',
  template: '<div class="el-table-stub"><slot /></div>',
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  template: '<div class="el-table-column-stub" />',
})

function mountView() {
  return mount(ActivityListView, {
    global: {
      directives: {
        loading: () => undefined,
      },
      stubs: {
        StatusBadge: true,
        Plus: true,
        Eye: true,
        Megaphone: true,
        PenSquare: true,
        SquareArrowOutUpRight: true,
        Trash2: true,
        'el-table': ElTableStub,
        'el-table-column': ElTableColumnStub,
      },
    },
  })
}

describe('ActivityListView', () => {
  it('renders an empty state instead of the table when there are no activities', async () => {
    vi.mocked(activityApi.list).mockResolvedValue([])

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('当前还没有活动')
    expect(wrapper.find('.el-table-stub').exists()).toBe(false)
  })

  it('renders the activity table when activities exist', async () => {
    const activity: ActivitySummary = {
      id: 1,
      title: '首发秒杀活动',
      totalStock: 100,
      availableStock: 80,
      publishMode: 'IMMEDIATE',
      publishStatus: 'UNPUBLISHED',
      phase: 'PREVIEW',
      publishTime: '2026-04-16T08:00:00',
      startTime: '2026-04-16T10:00:00',
      endTime: '2026-04-16T12:00:00',
    }

    vi.mocked(activityApi.list).mockResolvedValue([activity])

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.el-table-stub').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('当前还没有活动')
  })
})
