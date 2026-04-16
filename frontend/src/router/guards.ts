import type { UserProfile } from '@/types'

export interface RouteGuardInput {
  path: string
  accessToken: string
  currentUser: UserProfile | null
}

export interface RouteGuardDecision {
  allow: boolean
  redirectTo: string | null
}

export function evaluateRouteGuard(input: RouteGuardInput): RouteGuardDecision {
  const isAuthenticated = Boolean(input.accessToken)
  const isAdminRoute = input.path.startsWith('/admin')
  const isLoginRoute = input.path === '/login'
  const isAdminLike =
    input.currentUser?.role === 'ADMIN' || input.currentUser?.role === 'PUBLISHER'

  if (isLoginRoute && isAuthenticated) {
    return {
      allow: false,
      redirectTo: isAdminLike ? '/admin/activities' : '/public/home',
    }
  }

  if (isAdminRoute && !isAuthenticated) {
    return {
      allow: false,
      redirectTo: '/login',
    }
  }

  if (isAdminRoute && !isAdminLike) {
    return {
      allow: false,
      redirectTo: '/public/home',
    }
  }

  return {
    allow: true,
    redirectTo: null,
  }
}
