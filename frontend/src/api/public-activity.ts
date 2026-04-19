import { http } from '@/api/http'
import type { ActivityDetail, ActivitySummary } from '@/types'

export const publicActivityApi = {
  async list() {
    return http.get<ActivitySummary[]>('/api/public/activities')
  },
  async detail(activityId: number) {
    return http.get<ActivityDetail>(`/api/public/activities/${activityId}`)
  },
}
