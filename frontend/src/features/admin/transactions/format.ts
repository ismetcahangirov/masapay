// Display helpers for the transactions table (#45). Amounts arrive in major
// units (manat) and times as UTC instants; both are shown the Baku way.

import type { SplitType, TransactionStatus } from './transactions-api'

const LOCALE = 'az-AZ'

export const STATUS_LABELS: Record<TransactionStatus, string> = {
  PENDING: 'Gözləyir',
  SUCCESS: 'Uğurlu',
  FAILED: 'Uğursuz',
  REFUNDED: 'Geri qaytarılıb',
}

export const SPLIT_LABELS: Record<SplitType, string> = {
  FULL: 'Tam',
  ITEM: 'Məhsula görə',
  EQUAL: 'Bərabər',
}

export function formatAmount(amount: number, currency: string): string {
  return new Intl.NumberFormat(LOCALE, { style: 'currency', currency }).format(
    amount,
  )
}

const DATE_TIME = new Intl.DateTimeFormat(LOCALE, {
  timeZone: 'Asia/Baku',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function formatDateTime(instant: string): string {
  return DATE_TIME.format(new Date(instant))
}
