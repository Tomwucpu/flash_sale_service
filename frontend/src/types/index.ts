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

export interface UpdateProfilePayload {
  nickname?: string | null
  phone?: string | null
}

export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
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
  | 'EXCEED_STOCK_LIMIT'
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
  currentTotalImportedCount?: number
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

export interface UserPageParams {
  keyword?: string
  role?: string
  status?: string
  page?: number
  size?: number
}

export interface UserPageResponse {
  records: UserProfile[]
  total: number
  page: number
  size: number
}

export interface ActivityPageResponse {
  records: ActivitySummary[]
  total: number
  page: number
  size: number
  totalCount: number
  unpublishedCount: number
  publishedCount: number
}

export interface UpdateUserStatusPayload {
  status: 'ENABLED' | 'DISABLED'
}

export interface UpdateUserRolePayload {
  role: 'ADMIN' | 'PUBLISHER' | 'USER'
}

export interface PublisherApplication {
  id: number
  userId: number
  username: string
  reason: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  reviewNote: string | null
  reviewerId: number | null
  reviewerName: string | null
  reviewedAt: string | null
  createdAt: string
}

export interface ApplicationPageParams {
  status?: string
  page?: number
  size?: number
}

export interface ApplicationPageResponse {
  records: PublisherApplication[]
  total: number
  page: number
  size: number
}

export interface PublisherApplicationPayload {
  reason: string
}

export interface ApplicationReviewPayload {
  reviewNote?: string
}

export type DashboardGranularity = 'day' | 'week' | 'month'

export interface DashboardSummary {
  revenue: number
  revenueChangeRate: number
  avgOrderValue: number
  totalOrders: number
  totalOrdersChangeRate: number
  paidOrders: number
  paidOrdersChangeRate: number
  paidOrderRate: number
  inventoryConsumed: number
  inventoryTotal: number
  inventoryConsumptionRate: number
  highConsumptionActivityCount: number
  pendingCompensations: number
}

export interface DashboardTrendBucket {
  label: string
  startDate: string
  endDate: string
  revenue: number
  totalOrders: number
  paidOrders: number
  inventoryConsumptionRate: number
}

export interface DashboardTrend {
  granularity: DashboardGranularity
  periodLabel: string
  buckets: DashboardTrendBucket[]
}

export interface DashboardActivityPerformanceItem {
  activityId: number
  title: string
  phase: string
  revenue: number
  revenueChangeRate: number
  totalOrders: number
  totalOrdersChangeRate: number
  paidOrders: number
  paidOrderRate: number
  inventoryConsumptionRate: number
}

export interface DashboardInsights {
  highConsumptionCount: number
  mediumConsumptionCount: number
  lowConsumptionCount: number
  messages: string[]
}

export interface PublisherDashboard {
  summary: DashboardSummary
  trend: DashboardTrend
  activityPerformance: DashboardActivityPerformanceItem[]
  insights: DashboardInsights
}
