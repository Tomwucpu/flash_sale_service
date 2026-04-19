import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
  },
}))

const { http } = await import('@/api/http')
const { publicActivityApi } = await import('@/api/public-activity')

describe('publicActivityApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads public activity list from backend endpoint', async () => {
    vi.mocked(http.get).mockResolvedValue([])

    await publicActivityApi.list()

    expect(http.get).toHaveBeenCalledWith('/api/public/activities')
  })

  it('loads public activity detail from backend endpoint', async () => {
    vi.mocked(http.get).mockResolvedValue(null)

    await publicActivityApi.detail(1001)

    expect(http.get).toHaveBeenCalledWith('/api/public/activities/1001')
  })
})
