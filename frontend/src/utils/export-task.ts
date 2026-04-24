import type { ExportTaskCreatePayload, OrderDetail } from '@/types'

const soldCodeFilters = {
  orderStatus: 'CONFIRMED',
  codeStatus: 'ISSUED',
} as const

export function buildSoldCodeExportPayload(activityId: number): ExportTaskCreatePayload {
  return {
    activityId,
    format: 'CSV',
    filters: soldCodeFilters,
  }
}

export function canExportSoldCodes(orders: OrderDetail[]) {
  return orders.some((item) => (
    item.orderStatus === soldCodeFilters.orderStatus
    && item.codeStatus === soldCodeFilters.codeStatus
  ))
}

export function getExportFileName(fileUrl: string | null | undefined, fallback = 'activity-export.csv') {
  if (!fileUrl) {
    return fallback
  }

  const pathPart = fileUrl.split(/[?#]/)[0] ?? ''
  const rawFileName = pathPart.slice(pathPart.lastIndexOf('/') + 1)

  if (!rawFileName) {
    return fallback
  }

  try {
    return decodeURIComponent(rawFileName)
  } catch {
    return rawFileName
  }
}
