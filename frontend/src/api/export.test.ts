import { describe, expect, it, vi } from 'vitest'
import { exportApi } from '@/api/export'
import { http } from '@/api/http'
import type { ExportTaskCreatePayload } from '@/types'

vi.mock('@/api/http', () => ({
  http: {
    postDownloadBlob: vi.fn(),
  },
}))

describe('exportApi', () => {
  it('downloads export files with a POST blob request', async () => {
    const payload: ExportTaskCreatePayload = {
      activityId: 12,
      format: 'CSV',
      filters: {
        orderStatus: 'CONFIRMED',
        codeStatus: 'ISSUED',
      },
    }
    const blob = new Blob(['orderNo,code'])
    vi.mocked(http.postDownloadBlob).mockResolvedValue(blob)

    await expect(exportApi.downloadExport(payload)).resolves.toBe(blob)

    expect(http.postDownloadBlob).toHaveBeenCalledWith('/api/exports/files', payload)
  })
})
