export type ApiCode =
  | 'SUCCESS'
  | 'INVALID_ARGUMENT'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'SYSTEM_ERROR'
  | (string & {})

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

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
  phone?: string
}

export type PurchaseLimitType = 'SINGLE' | 'MULTI'
export type CodeSourceMode = 'SYSTEM_GENERATED' | 'THIRD_PARTY_IMPORTED'
export type PublishMode = 'IMMEDIATE' | 'SCHEDULED'
export type PublishStatus = 'UNPUBLISHED' | 'PUBLISHED' | 'OFFLINE'
export type ActivityPhase = 'PREVIEW' | 'ONGOING' | 'ENDED'
export type RedeemCodeImportFailureReason =
  | 'EMPTY_CODE'
  | 'INVALID_FORMAT'
  | 'DUPLICATE_IN_FILE'
  | 'DUPLICATE_IN_SYSTEM'
  | (string & {})

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

export interface SeckillAttemptResponse {
  activityId: number
  status: string
}

export interface SeckillResult {
  status: string
  orderNo: string | null
  message: string | null
  code: string | null
  updatedAt: string | null
}

export interface PaymentOrder {
  orderNo: string
  transactionNo: string
  payAmount: number
  payStatus: string
}

export interface PaymentCallbackPayload {
  orderNo: string
  transactionNo: string
}

export interface OrderDetail {
  orderNo: string
  activityId: number
  userId: number
  orderStatus: string
  payStatus: string
  codeStatus: string
  priceAmount: number
  failReason: string | null
  code: string | null
  updatedAt: string
}

export type ExportFormat = 'CSV' | 'XLSX'
export type ExportTaskStatus = 'INIT' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | (string & {})

export interface ExportTaskFilters {
  payStatus?: string
  orderStatus?: string
  codeStatus?: string
  userId?: number
}

export interface ExportTaskCreatePayload {
  activityId: number
  format: ExportFormat
  filters: ExportTaskFilters
}

export interface ExportTask {
  id: number
  activityId: number
  operatorId?: number
  format: ExportFormat
  filters: ExportTaskFilters
  status: ExportTaskStatus
  fileUrl?: string | null
  failReason?: string | null
  createdAt: string
  updatedAt: string
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
  publishTime?: string
  startTime: string
  endTime: string
}

export interface RedeemCodeImportFailure {
  lineNumber: number
  rawCode: string
  reason: RedeemCodeImportFailureReason
}

export interface RedeemCodeImportBatchSummary {
  batchNo: string
  fileName: string
  totalCount: number
  successCount: number
  failedCount: number
}

export interface RedeemCodeImportBatchDetail extends RedeemCodeImportBatchSummary {
  failures: RedeemCodeImportFailure[]
}
