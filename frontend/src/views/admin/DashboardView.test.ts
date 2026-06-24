import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, nextTick } from 'vue'
import DashboardView from '@/views/admin/DashboardView.vue'
import { dashboardApi } from '@/api/dashboard'
import type { PublisherDashboard } from '@/types'

const setOptionSpy = vi.fn()

vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    getPublisher: vi.fn(),
  },
}))

vi.mock('echarts', () => ({
  init: () => ({
    setOption: setOptionSpy,
    resize: vi.fn(),
    dispose: vi.fn(),
  }),
  graphic: {
    LinearGradient: class {},
  },
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<object>('element-plus')
  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
    },
  }
})

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

const ElSegmentedStub = defineComponent({
  name: 'ElSegmentedStub',
  props: {
    modelValue: {
      type: String,
      required: true,
    },
    options: {
      type: Array,
      required: true,
    },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () =>
      h(
        'div',
        { 'data-testid': 'granularity-switch' },
        (props.options as Array<{ label: string; value: string }>).map((option) =>
          h(
            'button',
            {
              type: 'button',
              'data-value': option.value,
              class: props.modelValue === option.value ? 'active' : '',
              onClick: () => emit('update:modelValue', option.value),
            },
            option.label,
          ),
        ),
      )
  },
})

function createDashboardResponse(): PublisherDashboard {
  return {
    summary: {
      revenue: 350,
      revenueChangeRate: 2.5,
      avgOrderValue: 116.67,
      totalOrders: 5,
      totalOrdersChangeRate: 0.6667,
      paidOrders: 3,
      paidOrdersChangeRate: 0.5,
      paidOrderRate: 0.6,
      inventoryConsumed: 205,
      inventoryTotal: 400,
      inventoryConsumptionRate: 0.5125,
      highConsumptionActivityCount: 1,
      pendingCompensations: 1,
    },
    trend: {
      granularity: 'week',
      periodLabel: '2026-05-04 至 2026-06-28',
      buckets: [
        {
          label: '06-15 至 06-21',
          startDate: '2026-06-15',
          endDate: '2026-06-21',
          revenue: 50,
          totalOrders: 1,
          paidOrders: 1,
          inventoryConsumptionRate: 0.5125,
        },
        {
          label: '06-22 至 06-28',
          startDate: '2026-06-22',
          endDate: '2026-06-28',
          revenue: 300,
          totalOrders: 4,
          paidOrders: 2,
          inventoryConsumptionRate: 0.5125,
        },
      ],
    },
    activityPerformance: [
      {
        activityId: 1,
        title: '活动A',
        phase: 'ONGOING',
        revenue: 300,
        revenueChangeRate: 2.75,
        totalOrders: 3,
        totalOrdersChangeRate: 0.5,
        paidOrders: 2,
        paidOrderRate: 0.6667,
        inventoryConsumptionRate: 0.9,
      },
    ],
    insights: {
      highConsumptionCount: 1,
      mediumConsumptionCount: 1,
      lowConsumptionCount: 1,
      messages: ['1 个高营收活动库存消耗超过 90%，需关注供给风险'],
    },
  }
}

describe('DashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setOptionSpy.mockReset()
  })

  it('keeps the trend chart layout stable', async () => {
    const getPublisher = vi.mocked(dashboardApi.getPublisher)
    getPublisher.mockResolvedValue(createDashboardResponse())

    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          ElSegmented: ElSegmentedStub,
        },
        directives: {
          loading: () => {},
        },
      },
    })

    await flushPromises()
    await nextTick()

    expect(getPublisher).toHaveBeenCalledWith('week')
    expect(setOptionSpy).toHaveBeenCalled()

    const chartOption = setOptionSpy.mock.calls.at(-1)?.[0]
    expect(chartOption.legend).toMatchObject({
      top: 28,
      data: ['支付订单', '营收'],
    })
    expect(chartOption.grid).toMatchObject({
      bottom: 40,
    })

    const progressBars = wrapper.findAll('[data-kind="inventory-progress"]')
    expect(progressBars.length).toBe(2)
    expect(wrapper.get('[data-testid="inventory-progress-summary"]').attributes('aria-valuenow')).toBe('51.25')
    expect(wrapper.get('[data-testid="inventory-progress-activity-1"]').attributes('aria-valuenow')).toBe('90.00')
  })

  it('surfaces dashboard highlights and semantic emphasis', async () => {
    const getPublisher = vi.mocked(dashboardApi.getPublisher)
    getPublisher.mockResolvedValue(createDashboardResponse())

    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          ElSegmented: ElSegmentedStub,
        },
        directives: {
          loading: () => {},
        },
      },
    })

    await flushPromises()
    await nextTick()

    const chips = wrapper.findAll('[data-testid="hero-highlight"]')
    expect(chips).toHaveLength(3)
    expect(chips[0]?.text()).toContain('营收环比')
    expect(chips[1]?.text()).toContain('支付转化')
    expect(chips[2]?.text()).toContain('待处理补偿')

    const summaryChanges = wrapper.findAll('[data-testid="summary-change"]')
    expect(summaryChanges).toHaveLength(3)
    expect(summaryChanges[0]?.attributes('data-tone')).toBe('positive')
    expect(summaryChanges[1]?.attributes('data-tone')).toBe('positive')
    expect(summaryChanges[2]?.attributes('data-tone')).toBe('warning')

    expect(wrapper.get('[data-testid="inventory-progress-summary"]').attributes('data-tone')).toBe('warning')
    expect(wrapper.get('[data-testid="inventory-progress-activity-1"]').attributes('data-tone')).toBe('danger')

    const fills = wrapper.findAll('.inventory-progress__fill')
    expect(fills[0]?.attributes('style')).toContain('background: var(--inventory-warning)')
    expect(fills[1]?.attributes('style')).toContain('background: var(--inventory-danger)')

    expect(wrapper.get('[data-testid="trend-summary"]').text()).toContain('营收高点')
    expect(wrapper.get('[data-testid="activity-phase-1"]').attributes('data-phase')).toBe('ongoing')
  })
})
