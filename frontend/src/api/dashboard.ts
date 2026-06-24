import { http } from '@/api/http'
import type { DashboardGranularity, PublisherDashboard } from '@/types'

export const dashboardApi = {
  getPublisher: (granularity: DashboardGranularity) =>
    http.get<PublisherDashboard>('/api/dashboard/publisher', {
      params: { granularity },
    }),
}
