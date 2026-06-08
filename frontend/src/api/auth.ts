import { http } from '@/api/http'
import type {
  ChangePasswordPayload,
  LoginPayload,
  LoginResponse,
  RegisterPayload,
  UpdateProfilePayload,
  UserProfile,
} from '@/types'

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
  updateProfile(payload: UpdateProfilePayload) {
    return http.put<UserProfile>('/api/users/me/profile', payload)
  },
  changePassword(payload: ChangePasswordPayload) {
    return http.put<void>('/api/users/me/password', payload)
  },
  getUserById(userId: number) {
    return http.get<UserProfile>(`/api/users/${userId}`)
  },
}
