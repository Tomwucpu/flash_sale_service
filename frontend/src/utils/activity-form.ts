import type { ActivityFormModel, ActivityFormPayload } from '@/types'
import { formatApiDateTime } from '@/utils/date'

export function toActivityPayload(form: ActivityFormModel): ActivityFormPayload {
  return {
    ...form,
    publishTime: formatApiDateTime(form.publishTime),
    startTime: formatApiDateTime(form.startTime),
    endTime: formatApiDateTime(form.endTime),
  }
}
