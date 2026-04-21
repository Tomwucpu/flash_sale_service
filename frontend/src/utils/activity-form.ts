import type { ActivityFormModel, ActivityFormPayload } from '@/types'
import { formatApiDateTime } from '@/utils/date'

export function toActivityPayload(form: ActivityFormModel): ActivityFormPayload {
  const needPayment = form.needPayment
  const purchaseLimitType = form.purchaseLimitType
  const publishMode = form.publishMode

  return {
    ...form,
    priceAmount: needPayment ? form.priceAmount : 0,
    purchaseLimitCount: purchaseLimitType === 'SINGLE' ? 1 : form.purchaseLimitCount,
    publishTime: publishMode === 'SCHEDULED' ? formatApiDateTime(form.publishTime) : undefined,
    startTime: formatApiDateTime(form.startTime),
    endTime: formatApiDateTime(form.endTime),
  }
}
