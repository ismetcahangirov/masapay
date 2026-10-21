import { api } from '@/app/api'
import { toMinor } from '@/features/order/order-api'
import type { DashboardMetrics } from '@/features/admin/dashboard/types'

// Dashboard KPIs of a restaurant (#43). The backend sends money as decimals
// (12.50); the UI works in minor units (1250), so the response is converted here.

interface DailyRevenueDto {
  date: string
  revenue: number
  tips: number
}

interface DashboardMetricsDto {
  restaurantId: string
  currency: string
  date: string
  timeZone: string
  revenueToday: number
  tipsToday: number
  averageTableMinutes: number | null
  payriffSuccessRate: number | null
  payriffSuccessCount: number
  payriffFailedCount: number
  series: DailyRevenueDto[]
}

/** "2026-10-21" to "21.10". */
function dayLabel(isoDate: string): string {
  const [, month, day] = isoDate.split('-')
  return `${day}.${month}`
}

function toMetrics(dto: DashboardMetricsDto): DashboardMetrics {
  return {
    currency: dto.currency,
    dailyRevenueMinor: toMinor(dto.revenueToday),
    tipsCollectedMinor: toMinor(dto.tipsToday),
    averageTableMinutes: dto.averageTableMinutes,
    payriffSuccessRate: dto.payriffSuccessRate,
    revenueSeries: dto.series.map((point) => ({
      date: point.date,
      label: dayLabel(point.date),
      revenueMinor: toMinor(point.revenue),
      tipsMinor: toMinor(point.tips),
    })),
  }
}

export const dashboardApi = api.injectEndpoints({
  endpoints: (build) => ({
    dashboardMetrics: build.query<
      DashboardMetrics,
      { restaurantId: string; days?: number }
    >({
      query: ({ restaurantId, days = 7 }) => ({
        url: `/admin/restaurants/${restaurantId}/dashboard`,
        params: { days },
      }),
      transformResponse: (dto: DashboardMetricsDto) => toMetrics(dto),
    }),
  }),
})

export const { useDashboardMetricsQuery } = dashboardApi
