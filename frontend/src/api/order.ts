import { http } from '@/api/http'
import type { OrderDetail } from '@/types'

export const orderApi = {
  listMine() {
    return http.get<OrderDetail[]>('/api/orders')
  },
  listActivityOrdersForPublisher(activityId: number) {
    return http.get<OrderDetail[]>(`/api/orders/admin/activities/${activityId}`)
  },
}
