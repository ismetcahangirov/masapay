import type { SubscriptionStatus } from './platform-api'

// Subscription dates are plain "YYYY-MM-DD" days in Baku time (#148), the same
// way the backend counts them.

const DAY_MS = 24 * 60 * 60 * 1000

/** Today in Baku as "YYYY-MM-DD". */
export function bakuToday(now: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Baku' }).format(now)
}

/** Days from `from` to `to`; 0 when they are the same day. */
export function daysBetween(from: string, to: string): number {
  return Math.round((Date.parse(to) - Date.parse(from)) / DAY_MS)
}

/** `day` plus whole months, e.g. 2026-01-31 + 1 month = 2026-02-28. */
export function addMonths(day: string, months: number): string {
  const [y, m, d] = day.split('-').map(Number)
  const lastDay = new Date(Date.UTC(y, m - 1 + months + 1, 0)).getUTCDate()
  return new Date(Date.UTC(y, m - 1 + months, Math.min(d, lastDay)))
    .toISOString()
    .slice(0, 10)
}

/** "2026-10-23" as "23.10.2026". */
export function formatDay(day: string): string {
  const [y, m, d] = day.split('-')
  return `${d}.${m}.${y}`
}

export const STATUS_LABELS: Record<SubscriptionStatus, string> = {
  NONE: 'Abunəlik yoxdur',
  TRIAL: 'Sınaq',
  ACTIVE: 'Davam edir',
  EXPIRED: 'Bitib',
}

/** A manager is warned this many days before the last day. */
export const WARN_DAYS = 7
