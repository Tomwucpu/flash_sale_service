import type { ActivityPhase, ActivitySummary, CodeSourceMode, PublishMode, PublishStatus, PurchaseLimitType } from '@/types'

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

export function getPublishStatusLabel(status: PublishStatus) {
  return publishStatusLabelMap[status]
}

export function getPhaseLabel(phase: ActivityPhase) {
  return phaseLabelMap[phase]
}

export function isEditableActivity(activity: Pick<ActivitySummary, 'publishStatus'>) {
  return activity.publishStatus === 'UNPUBLISHED'
}
