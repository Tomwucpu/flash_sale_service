import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { LoginPayload, LoginResponse, UserProfile } from '@/types'

interface AuthState {
  accessToken: string
  currentUser: UserProfile | null
}

export const AUTH_STORAGE_KEY = 'flash-sale-auth'

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: '',
    currentUser: null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken),
    isAdminLike: (state) =>
      state.currentUser?.role === 'ADMIN' || state.currentUser?.role === 'PUBLISHER',
  },
  actions: {
    persistSession() {
      if (!this.accessToken || !this.currentUser) {
        localStorage.removeItem(AUTH_STORAGE_KEY)
        return
      }

      localStorage.setItem(
        AUTH_STORAGE_KEY,
        JSON.stringify({
          accessToken: this.accessToken,
          user: this.currentUser,
        } satisfies LoginResponse),
      )
    },
    hydrateFromStorage() {
      const raw = localStorage.getItem(AUTH_STORAGE_KEY)
      if (!raw) {
        this.clearSession()
        return
      }

      try {
        const parsed = JSON.parse(raw) as LoginResponse
        this.accessToken = parsed.accessToken
        this.currentUser = parsed.user
      } catch {
        this.clearSession()
      }
    },
    setSession(payload: LoginResponse) {
      this.accessToken = payload.accessToken
      this.currentUser = payload.user
      this.persistSession()
    },
    clearSession() {
      this.accessToken = ''
      this.currentUser = null
      localStorage.removeItem(AUTH_STORAGE_KEY)
    },
    async login(payload: LoginPayload) {
      const session = await authApi.login(payload)
      this.setSession(session)
      return session
    },
    async fetchMe() {
      if (!this.accessToken) {
        return null
      }

      const profile = await authApi.me()
      this.currentUser = profile
      this.persistSession()
      return profile
    },
    logout() {
      this.clearSession()
    },
  },
})
