export type ApiCode = 'SUCCESS' | 'INVALID_ARGUMENT' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'SYSTEM_ERROR'

export interface ApiResponse<T> {
  code: ApiCode
  message: string
  requestId: string | null
  data: T | null
}

export interface UserProfile {
  id: number
  username: string
  role: 'ADMIN' | 'PUBLISHER' | 'USER'
  status: 'ENABLED' | 'DISABLED'
  nickname: string | null
  phone: string | null
}

export interface LoginResponse {
  accessToken: string
  user: UserProfile
}

export interface LoginPayload {
  username: string
  password: string
}

export type PurchaseLimitType = 'SINGLE' | 'MULTI'
export type CodeSourceMode = 'SYSTEM_GENERATED' | 'THIRD_PARTY_IMPORTED'
export type PublishMode = 'IMMEDIATE' | 'SCHEDULED'
export type PublishStatus = 'UNPUBLISHED' | 'PUBLISHED' | 'OFFLINE'
export type ActivityPhase = 'PREVIEW' | 'ONGOING' | 'ENDED'

export interface ActivitySummary {
  id: number
  title: string
  totalStock: number
  availableStock: number
  publishMode: PublishMode
  publishStatus: PublishStatus
  phase: ActivityPhase
  publishTime: string
  startTime: string
  endTime: string
}

export interface ActivityDetail extends ActivitySummary {
  description: string
  coverUrl: string
  priceAmount: number
  needPayment: boolean
  purchaseLimitType: PurchaseLimitType
  purchaseLimitCount: number
  codeSourceMode: CodeSourceMode
}

export interface ActivityFormModel {
  title: string
  description: string
  coverUrl: string
  totalStock: number
  priceAmount: number
  needPayment: boolean
  purchaseLimitType: PurchaseLimitType
  purchaseLimitCount: number
  codeSourceMode: CodeSourceMode
  publishMode: PublishMode
  publishTime: Date | null
  startTime: Date | null
  endTime: Date | null
}

export interface ActivityFormPayload {
  title: string
  description: string
  coverUrl: string
  totalStock: number
  priceAmount: number
  needPayment: boolean
  purchaseLimitType: PurchaseLimitType
  purchaseLimitCount: number
  codeSourceMode: CodeSourceMode
  publishMode: PublishMode
  publishTime: string
  startTime: string
  endTime: string
}
