// Display helpers for the transactions table (#45). Amounts arrive in major
// units (manat) and times as UTC instants; both are shown the Baku way.

import i18n, { currentLocale } from '@/i18n'
import type { SplitType, TransactionStatus } from './transactions-api'

// The labels are getters, so each read is in the language chosen at that moment.
export const STATUS_LABELS: Record<TransactionStatus, string> = {
  get PENDING() {
    return i18n.t('transactions:status.PENDING')
  },
  get SUCCESS() {
    return i18n.t('transactions:status.SUCCESS')
  },
  get FAILED() {
    return i18n.t('transactions:status.FAILED')
  },
  get REFUNDED() {
    return i18n.t('transactions:status.REFUNDED')
  },
}

export const SPLIT_LABELS: Record<SplitType, string> = {
  get FULL() {
    return i18n.t('transactions:split.FULL')
  },
  get ITEM() {
    return i18n.t('transactions:split.ITEM')
  },
  get EQUAL() {
    return i18n.t('transactions:split.EQUAL')
  },
}

export function formatAmount(amount: number, currency: string): string {
  return new Intl.NumberFormat(currentLocale(), {
    style: 'currency',
    currency,
  }).format(amount)
}

export function formatDateTime(instant: string): string {
  return new Intl.DateTimeFormat(currentLocale(), {
    timeZone: 'Asia/Baku',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(instant))
}
