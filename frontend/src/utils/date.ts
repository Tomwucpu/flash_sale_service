import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'

dayjs.extend(customParseFormat)

export function formatApiDateTime(value: Date | null) {
  if (!value) {
    return ''
  }

  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}

export function parseApiDateTime(value: string | null | undefined) {
  if (!value) {
    return null
  }

  const primary = dayjs(value)
  if (primary.isValid()) {
    return primary.toDate()
  }

  const fallback = dayjs(value, 'YYYY-MM-DD HH:mm:ss')
  return fallback.isValid() ? fallback.toDate() : null
}

export function formatDisplayDateTime(value: string | Date | null | undefined) {
  if (!value) {
    return '未设置'
  }

  return dayjs(value).format('YYYY.MM.DD HH:mm')
}
