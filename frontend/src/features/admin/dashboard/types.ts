// Admin dashboard metrics.
//
// Consumed by presentational KPI cards and the revenue chart. Produced by the
// admin analytics endpoints once EPIC 2 (data) and EPIC 7 (Payriff) land; the
// shapes here define that contract.

export interface RevenuePoint {
  /** Bucket label, e.g. an hour ("09") or a weekday. */
  label: string
  /** Revenue for the bucket, in minor currency units (qəpik). */
  revenueMinor: number
}

export interface DashboardMetrics {
  currency: string
  /** Total revenue for the day, in minor units. */
  dailyRevenueMinor: number
  /** Tips collected for the day, in minor units. */
  tipsCollectedMinor: number
  /** Average time a table stays open, in minutes. */
  averageTableMinutes: number
  /** Share of successful Payriff payments, from 0 to 1. */
  payriffSuccessRate: number
  /** Revenue broken down over the day for the chart. */
  revenueSeries: RevenuePoint[]
}
