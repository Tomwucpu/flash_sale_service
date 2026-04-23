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
  const isUserRoute = input.path.startsWith('/user')
  const isAuthPage = input.path === '/login' || input.path === '/register'
  const isAdminLike =
    input.currentUser?.role === 'ADMIN' || input.currentUser?.role === 'PUBLISHER'

  if (isAuthPage && isAuthenticated) {
    return {
      allow: false,
      redirectTo: isAdminLike ? '/admin/activities' : '/user/orders',
    }
  }

  if ((isAdminRoute || isUserRoute) && !isAuthenticated) {
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
