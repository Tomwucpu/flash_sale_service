import { http } from '@/api/http'
import type { ExportTask, ExportTaskCreatePayload } from '@/types'

export const exportApi = {
  createTask(payload: ExportTaskCreatePayload) {
    return http.post<ExportTask>('/api/exports/tasks', payload)
  },
  getTask(taskId: number) {
    return http.get<ExportTask>(`/api/exports/tasks/${taskId}`)
  },
  downloadFile(fileName: string) {
    return http.downloadBlob(`/api/exports/files/${encodeURIComponent(fileName)}`)
  },
}
