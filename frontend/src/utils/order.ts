import type { OrderDetail } from '@/types'

export type StatusTone = 'blue' | 'green' | 'amber' | 'slate'

const orderStatusLabels: Record<string, string> = {
  INIT: '处理中',
  CONFIRMED: '已确认',
  CLOSED: '已关闭',
  FAILED: '失败',
}

const payStatusLabels: Record<string, string> = {
  NO_NEED: '免支付',
  WAIT_PAY: '待支付',
  PAID: '已支付',
  CLOSED: '已关闭',
}

const codeStatusLabels: Record<string, string> = {
  PENDING: '待发码',
  ISSUED: '已发码',
  FAILED: '发码失败',
}

export function getOrderStatusLabel(value: string) {
  return orderStatusLabels[value] ?? value
}

export function getPayStatusLabel(value: string) {
  return payStatusLabels[value] ?? value
}

export function getCodeStatusLabel(value: string) {
  return codeStatusLabels[value] ?? value
}

export function orderStatusTone(value: string): StatusTone {
  if (value === 'CONFIRMED') {
    return 'green'
  }
  if (value === 'INIT') {
    return 'blue'
  }
  if (value === 'FAILED') {
    return 'amber'
  }
  return 'slate'
}

export function payStatusTone(value: string): StatusTone {
  if (value === 'PAID' || value === 'NO_NEED') {
    return 'green'
  }
  if (value === 'WAIT_PAY') {
    return 'amber'
  }
  return 'slate'
}

export function codeStatusTone(value: string): StatusTone {
  if (value === 'ISSUED') {
    return 'green'
  }
  if (value === 'PENDING') {
    return 'blue'
  }
  return 'amber'
}

export function formatOrderAmount(value: number) {
  const amount = Number(value)
  return amount > 0 ? `￥${amount.toFixed(2)}` : '免支付'
}

export function summarizeActivityOrders(orders: OrderDetail[]) {
  return {
    total: orders.length,
    confirmed: orders.filter((item) => item.orderStatus === 'CONFIRMED').length,
    issuedCodes: orders.filter((item) => item.codeStatus === 'ISSUED').length,
    waitingPayment: orders.filter((item) => item.payStatus === 'WAIT_PAY').length,
    closedOrFailed: orders.filter((item) => item.orderStatus === 'CLOSED' || item.orderStatus === 'FAILED').length,
  }
}
