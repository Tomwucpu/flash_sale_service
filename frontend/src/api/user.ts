import { http } from '@/api/http'
import type { UpdateUserRolePayload, UpdateUserStatusPayload, UserPageParams, UserPageResponse, UserProfile } from '@/types'

export const userApi = {
  list(params?: UserPageParams) {
    return http.get<UserPageResponse>('/api/admin/users', { params })
  },
  updateStatus(userId: number, payload: UpdateUserStatusPayload) {
    return http.put<UserProfile>(`/api/admin/users/${userId}/status`, payload)
  },
  updateRole(userId: number, payload: UpdateUserRolePayload) {
    return http.put<UserProfile>(`/api/admin/users/${userId}/role`, payload)
  },
}
