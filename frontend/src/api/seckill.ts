import { http } from '@/api/http'
import type {
  ApiResponse,
  OrderDetail,
  PaymentCallbackPayload,
  PaymentOrder,
  SeckillAttemptResponse,
  SeckillResult,
} from '@/types'

export const seckillApi = {
  attempt(activityId: number) {
    return http.postEnvelope<SeckillAttemptResponse>(
      `/api/seckill/activities/${activityId}/attempt`,
    ) as Promise<ApiResponse<SeckillAttemptResponse>>
  },
  queryResult(activityId: number) {
    return http.get<SeckillResult>(`/api/seckill/results/${activityId}`)
  },
  createPayment(orderNo: string) {
    return http.post<PaymentOrder>(`/api/payments/orders/${orderNo}`)
  },
  callbackPayment(payload: PaymentCallbackPayload) {
    return http.post<PaymentOrder>('/api/payments/callback', payload)
  },
  queryActivityOrders(activityId: number) {
    return http.get<OrderDetail[]>(`/api/orders/activities/${activityId}`)
  },
}
