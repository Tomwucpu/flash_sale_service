import type { ApiCode, ApiResponse } from '@/types'

export class ApiClientError extends Error {
  code: ApiCode
  status: number
  requestId: string | null

  constructor(message: string, code: ApiCode, status: number, requestId: string | null = null) {
    super(message)
    this.name = 'ApiClientError'
    this.code = code
    this.status = status
    this.requestId = requestId
  }
}

export function createRequestId() {
  const now = new Date()
  const date = [
    now.getFullYear().toString().padStart(4, '0'),
    (now.getMonth() + 1).toString().padStart(2, '0'),
    now.getDate().toString().padStart(2, '0'),
  ].join('')
  const time = [
    now.getHours().toString().padStart(2, '0'),
    now.getMinutes().toString().padStart(2, '0'),
    now.getSeconds().toString().padStart(2, '0'),
  ].join('')
  const suffix = Math.random().toString(36).slice(2, 6).toUpperCase().padEnd(4, '0')

  return `REQ-${date}-${time}-${suffix}`
}

export function unwrapApiResponse<T>(
  response: { status: number; data: ApiResponse<T> },
  onUnauthorized?: () => void,
) {
  if (response.data.code === 'SUCCESS') {
    return response.data.data as T
  }

  if (response.status === 401 && onUnauthorized) {
    onUnauthorized()
  }

  throw new ApiClientError(
    response.data.message,
    response.data.code,
    response.status,
    response.data.requestId,
  )
}
