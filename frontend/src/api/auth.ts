import { http } from '@/api/http'
import type { LoginPayload, LoginResponse, UserProfile } from '@/types'

export const authApi = {
  login(payload: LoginPayload) {
    return http.post<LoginResponse>('/api/users/login', payload)
  },
  me() {
    return http.get<UserProfile>('/api/users/me')
  },
  getUserById(userId: number) {
    return http.get<UserProfile>(`/api/users/${userId}`)
  },
}
