import { publicActivitiesMock } from '@/mocks/publicActivities'
import type { ActivityDetail, ActivitySummary } from '@/types'

function toSummary(activity: ActivityDetail): ActivitySummary {
  return {
    id: activity.id,
    title: activity.title,
    totalStock: activity.totalStock,
    availableStock: activity.availableStock,
    publishMode: activity.publishMode,
    publishStatus: activity.publishStatus,
    phase: activity.phase,
    publishTime: activity.publishTime,
    startTime: activity.startTime,
    endTime: activity.endTime,
  }
}

export const publicActivityApi = {
  async list() {
    return publicActivitiesMock.map(toSummary)
  },
  async detail(activityId: number) {
    return publicActivitiesMock.find((activity) => activity.id === activityId) ?? null
  },
}
