import type {
  ActivityPhase,
  ActivitySummary,
  CodeSourceMode,
  PublishMode,
  PublishStatus,
  PurchaseLimitType,
  RedeemCodeImportFailureReason,
} from '@/types'

export const purchaseLimitOptions: Array<{ label: string; value: PurchaseLimitType }> = [
  { label: '单人一次', value: 'SINGLE' },
  { label: '单人多次', value: 'MULTI' },
]

export const codeSourceOptions: Array<{ label: string; value: CodeSourceMode }> = [
  { label: '系统生成', value: 'SYSTEM_GENERATED' },
  { label: '第三方导入', value: 'THIRD_PARTY_IMPORTED' },
]

export const publishModeOptions: Array<{ label: string; value: PublishMode }> = [
  { label: '立即发布', value: 'IMMEDIATE' },
  { label: '定时发布', value: 'SCHEDULED' },
]

const publishStatusLabelMap: Record<PublishStatus, string> = {
  UNPUBLISHED: '未发布',
  PUBLISHED: '已发布',
  OFFLINE: '已下线',
}

const phaseLabelMap: Record<ActivityPhase, string> = {
  PREVIEW: '预告中',
  ONGOING: '进行中',
  ENDED: '已结束',
}

const codeSourceModeLabelMap: Record<CodeSourceMode, string> = {
  SYSTEM_GENERATED: '系统生成',
  THIRD_PARTY_IMPORTED: '第三方导入',
}

const publishModeLabelMap: Record<PublishMode, string> = {
  IMMEDIATE: '立即发布',
  SCHEDULED: '定时发布',
}

const importFailureReasonLabelMap: Record<string, string> = {
  EMPTY_CODE: '空白兑换码',
  INVALID_FORMAT: '兑换码格式非法',
  DUPLICATE_IN_FILE: '文件内重复',
  DUPLICATE_IN_SYSTEM: '系统内已存在',
}

export function getPublishStatusLabel(status: PublishStatus) {
  return publishStatusLabelMap[status]
}

export function getPhaseLabel(phase: ActivityPhase) {
  return phaseLabelMap[phase]
}

export function getCodeSourceModeLabel(mode: CodeSourceMode) {
  return codeSourceModeLabelMap[mode]
}

export function getPublishModeLabel(mode: PublishMode) {
  return publishModeLabelMap[mode]
}

export function getImportFailureReasonLabel(reason: RedeemCodeImportFailureReason) {
  return importFailureReasonLabelMap[reason] ?? reason
}

export function isEditableActivity(activity: Pick<ActivitySummary, 'publishStatus'>) {
  return activity.publishStatus === 'UNPUBLISHED'
}

export function isDeletableActivity(activity: Pick<ActivitySummary, 'publishStatus'>) {
  return activity.publishStatus === 'UNPUBLISHED' || activity.publishStatus === 'OFFLINE'
}

export function shouldShowCodeImportPanel(activity: Pick<{ codeSourceMode: CodeSourceMode }, 'codeSourceMode'>) {
  return activity.codeSourceMode === 'THIRD_PARTY_IMPORTED'
}
