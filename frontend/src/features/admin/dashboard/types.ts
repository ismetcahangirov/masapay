// Admin dashboard metrics (#43).
//
// Consumed by presentational KPI cards and the revenue chart; filled from
// GET /admin/restaurants/{id}/dashboard (see dashboard-api.ts).

export interface RevenuePoint {
  /** Calendar day in Asia/Baku, ISO "2026-10-21". */
  date: string
  /** Short axis label, e.g. "21.10". */
  label: string
  /** Revenue of the day excluding tips, in minor currency units (qəpik). */
  revenueMinor: number
  /** Tips of the day, in minor units. */
  tipsMinor: number
}

export interface DashboardMetrics {
  currency: string
  /** Today's revenue excluding tips, in minor units. */
  dailyRevenueMinor: number
  /** Tips collected today, in minor units. */
  tipsCollectedMinor: number
  /** Average time a table stays open, in minutes; null when no bill was paid today. */
  averageTableMinutes: number | null
  /** Share of successful Payriff payments, from 0 to 1; null when there were none. */
  payriffSuccessRate: number | null
  /** Revenue per day, oldest first, ending with today. */
  revenueSeries: RevenuePoint[]
}
