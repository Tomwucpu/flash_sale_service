import { http } from '@/api/http'
import type { LoginPayload, LoginResponse, RegisterPayload, UserProfile } from '@/types'

export const authApi = {
  register(payload: RegisterPayload) {
    return http.post<UserProfile>('/api/users/register', payload)
  },
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
