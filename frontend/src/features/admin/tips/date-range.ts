import type { WaiterTips } from '@/features/admin/tips/tips-api'

// Date helpers for the tip report filter. Dates are plain ISO days
// ("2026-10-21") in the browser's local calendar, which the backend reads as
// Asia/Baku days.

export interface DateRange {
  from: string
  to: string
}

export type RangePreset = 'today' | 'week' | 'month'

/** Local calendar day as "YYYY-MM-DD" (toISOString would shift it to UTC). */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

/** Today, this week (from Monday) or this month, each up to today. */
export function presetRange(
  preset: RangePreset,
  today: Date = new Date(),
): DateRange {
  const start = new Date(today)
  if (preset === 'week') {
    // getDay(): Sunday is 0, so Monday-based weeks go back 6 days on Sunday.
    start.setDate(today.getDate() - ((today.getDay() + 6) % 7))
  } else if (preset === 'month') {
    start.setDate(1)
  }
  return { from: toIsoDate(start), to: toIsoDate(today) }
}

/** The preset a range matches, if any, so its button shows as selected. */
export function matchingPreset(range: DateRange): RangePreset | undefined {
  return (['today', 'week', 'month'] as const).find((preset) => {
    const candidate = presetRange(preset)
    return candidate.from === range.from && candidate.to === range.to
  })
}

/** Display name of a waiter row; tips without a waiter are grouped as unassigned. */
export function waiterLabel(waiter: WaiterTips): string {
  if (!waiter.waiterId) return 'Təyin olunmayıb'
  return waiter.waiterName || waiter.waiterEmail || 'Adsız ofisiant'
}
