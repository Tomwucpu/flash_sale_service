import { http } from '@/api/http'
import type { ActivityDetail, ActivityPageResponse, ActivityPhase } from '@/types'

export const publicActivityApi = {
  async list(page = 1, size = 10, phase?: ActivityPhase) {
    return http.get<ActivityPageResponse>('/api/public/activities', { params: { page, size, phase: phase ?? undefined } })
  },
  async detail(activityId: number) {
    return http.get<ActivityDetail>(`/api/public/activities/${activityId}`)
  },
}
