import { http } from '@/api/http'
import type {
  ApplicationPageResponse,
  ApplicationReviewPayload,
  ApplicationPageParams,
  PublisherApplication,
  PublisherApplicationPayload,
} from '@/types'

export const applicationApi = {
  apply(payload: PublisherApplicationPayload) {
    return http.post<PublisherApplication>('/api/users/publisher-application', payload)
  },
  getMyApplication() {
    return http.get<PublisherApplication>('/api/users/publisher-application/me')
  },
  list(params?: ApplicationPageParams) {
    return http.get<ApplicationPageResponse>('/api/admin/users/publisher-applications', { params })
  },
  approve(applicationId: number, payload?: ApplicationReviewPayload) {
    return http.put<PublisherApplication>(
      `/api/admin/users/publisher-applications/${applicationId}/approve`,
      payload ?? {}
    )
  },
  reject(applicationId: number, payload?: ApplicationReviewPayload) {
    return http.put<PublisherApplication>(
      `/api/admin/users/publisher-applications/${applicationId}/reject`,
      payload ?? {}
    )
  },
}
