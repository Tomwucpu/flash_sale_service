import { http } from '@/api/http'
import type { ActivityDetail, ActivityFormPayload, ActivitySummary, RedeemCodeImportBatchDetail, RedeemCodeImportBatchSummary } from '@/types'

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
  advancePublish(activityId: number) {
    return http.post<ActivityDetail>(`/api/activities/${activityId}/advance-publish`)
  },
  offline(activityId: number) {
    return http.post<ActivityDetail>(`/api/activities/${activityId}/offline`)
  },
  delete(activityId: number) {
    return http.delete<void>(`/api/activities/${activityId}`)
  },
  importCodes(activityId: number, file: File) {
    const formData = new FormData()
    formData.append('file', file)

    return http.post<RedeemCodeImportBatchDetail>(`/api/activities/${activityId}/codes/import`, formData)
  },
  listImportBatches(activityId: number) {
    return http.get<RedeemCodeImportBatchSummary[]>(`/api/activities/${activityId}/codes/import-batches`)
  },
  importBatchDetail(activityId: number, batchNo: string) {
    return http.get<RedeemCodeImportBatchDetail>(`/api/activities/${activityId}/codes/import-batches/${encodeURIComponent(batchNo)}`)
  },
}
