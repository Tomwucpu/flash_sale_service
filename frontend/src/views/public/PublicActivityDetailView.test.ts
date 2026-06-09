import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PublicActivityDetailView from './PublicActivityDetailView.vue'
import { publicActivityApi } from '@/api/public-activity'
import { seckillApi } from '@/api/seckill'
import { useAuthStore } from '@/stores/auth'
import type { ActivityDetail } from '@/types'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: {
      id: '12',
    },
  }),
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

vi.mock('@/api/public-activity', () => ({
  publicActivityApi: {
    detail: vi.fn(),
  },
}))

vi.mock('@/api/seckill', () => ({
  seckillApi: {
    attempt: vi.fn(),
    queryResult: vi.fn(),
    queryActivityOrders: vi.fn(),
    createPayment: vi.fn(),
    callbackPayment: vi.fn(),
  },
}))

const activityDetail: ActivityDetail = {
  id: 12,
  title: 'Zero Stock Activity',
  description: 'No stock left',
  coverUrl: '/cover.png',
  totalStock: 10,
  availableStock: 0,
  publishMode: 'IMMEDIATE',
  publishStatus: 'PUBLISHED',
  phase: 'ONGOING',
  publishTime: '2026-06-09T10:00:00',
  startTime: '2026-06-09T10:00:00',
  endTime: '2026-06-09T12:00:00',
  priceAmount: 19.9,
  needPayment: false,
  purchaseLimitType: 'SINGLE',
  purchaseLimitCount: 1,
  codeSourceMode: 'SYSTEM_GENERATED',
}

describe('PublicActivityDetailView', () => {
  beforeEach(() => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore().setSession({
      accessToken: 'token',
      user: {
        id: 1,
        username: 'buyer',
        role: 'USER',
        status: 'ENABLED',
        nickname: null,
        phone: null,
      },
    })

    vi.mocked(publicActivityApi.detail).mockResolvedValue(activityDetail)
    vi.mocked(seckillApi.queryResult).mockResolvedValue({
      status: 'INIT',
      orderNo: null,
      message: null,
      code: null,
      updatedAt: null,
    })
    vi.mocked(seckillApi.queryActivityOrders).mockResolvedValue([])
  })

  it('disables the seckill button and shows no-stock text when available stock is zero', async () => {
    const wrapper = mount(PublicActivityDetailView, {
      global: {
        plugins: [createPinia()],
        directives: {
          loading: {},
        },
        stubs: {
          ElTable: true,
          ElTableColumn: true,
        },
      },
    })

    await flushPromises()

    const button = wrapper.get('.detail-hero__seckill-button')

    expect(button.text()).toBe('暂无库存')
    expect(button.attributes('disabled')).toBeDefined()
  })
})
