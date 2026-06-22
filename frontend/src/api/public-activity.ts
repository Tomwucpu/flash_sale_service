import { http } from '@/api/http'
import type { ActivityDetail, ActivityPageResponse } from '@/types'

export const publicActivityApi = {
  async list(page = 1, size = 10) {
    return http.get<ActivityPageResponse>('/api/public/activities', { params: { page, size } })
  },
  async detail(activityId: number) {
    return http.get<ActivityDetail>(`/api/public/activities/${activityId}`)
  },
}
