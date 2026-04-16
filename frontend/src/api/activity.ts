import { http } from '@/api/http'
import type { ActivityDetail, ActivityFormPayload, ActivitySummary } from '@/types'

export const activityApi = {
  list() {
    return http.get<ActivitySummary[]>('/api/activities')
  },
  detail(activityId: number) {
    return http.get<ActivityDetail>(`/api/activities/${activityId}`)
  },
  create(payload: ActivityFormPayload) {
    return http.post<ActivityDetail>('/api/activities', payload)
  },
  update(activityId: number, payload: ActivityFormPayload) {
    return http.put<ActivityDetail>(`/api/activities/${activityId}`, payload)
  },
  publish(activityId: number) {
    return http.post<ActivityDetail>(`/api/activities/${activityId}/publish`)
  },
  offline(activityId: number) {
    return http.post<ActivityDetail>(`/api/activities/${activityId}/offline`)
  },
}
