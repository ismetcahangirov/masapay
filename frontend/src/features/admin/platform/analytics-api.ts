import { api } from '@/app/api'
import { toMinor } from '@/features/order/order-api'
import type { SubscriptionStatus } from './platform-api'

// Platform analytics for the super admin (#149). Amounts arrive as decimals
// and are kept in minor units here, like the restaurant dashboard does.

interface TotalsDto {
  currency: string
  restaurantCount: number
  activeRestaurantCount: number
  paymentCount: number
  revenue: number
  tips: number
  subscriptionRevenue: number
}

interface RowDto {
  id: string
  name: string
  currency: string
  active: boolean
  subscriptionStatus: SubscriptionStatus
  paymentCount: number
  revenue: number
  tips: number
  averagePayment: number | null
  subscriptionRevenue: number
}

interface PlatformAnalyticsDto {
  from: string
  to: string
  timeZone: string
  totals: TotalsDto[]
  restaurants: RowDto[]
}

/** One line per currency: amounts in different currencies are never added up. */
export interface CurrencyTotals {
  currency: string
  restaurantCount: number
  activeRestaurantCount: number
  paymentCount: number
  revenueMinor: number
  tipsMinor: number
  subscriptionRevenueMinor: number
}

export interface RestaurantRow {
  id: string
  name: string
  currency: string
  active: boolean
  subscriptionStatus: SubscriptionStatus
  paymentCount: number
  revenueMinor: number
  tipsMinor: number
  averagePaymentMinor: number | null
  subscriptionRevenueMinor: number
}

export interface PlatformAnalytics {
  /** "YYYY-MM-DD", both days included, Baku time. */
  from: string
  to: string
  totals: CurrencyTotals[]
  /** Biggest turnover first. */
  restaurants: RestaurantRow[]
}

function toAnalytics(dto: PlatformAnalyticsDto): PlatformAnalytics {
  return {
    from: dto.from,
    to: dto.to,
    totals: dto.totals.map((t) => ({
      currency: t.currency,
      restaurantCount: t.restaurantCount,
      activeRestaurantCount: t.activeRestaurantCount,
      paymentCount: t.paymentCount,
      revenueMinor: toMinor(t.revenue),
      tipsMinor: toMinor(t.tips),
      subscriptionRevenueMinor: toMinor(t.subscriptionRevenue),
    })),
    restaurants: dto.restaurants.map((r) => ({
      id: r.id,
      name: r.name,
      currency: r.currency,
      active: r.active,
      subscriptionStatus: r.subscriptionStatus,
      paymentCount: r.paymentCount,
      revenueMinor: toMinor(r.revenue),
      tipsMinor: toMinor(r.tips),
      averagePaymentMinor:
        r.averagePayment === null ? null : toMinor(r.averagePayment),
      subscriptionRevenueMinor: toMinor(r.subscriptionRevenue),
    })),
  }
}

export const analyticsApi = api.injectEndpoints({
  endpoints: (build) => ({
    platformAnalytics: build.query<
      PlatformAnalytics,
      { from: string; to: string }
    >({
      query: (range) => ({ url: '/admin/platform/analytics', params: range }),
      transformResponse: (dto: PlatformAnalyticsDto) => toAnalytics(dto),
      // A restaurant opened, switched off or given a subscription shows here too.
      providesTags: [{ type: 'Platform', id: 'restaurants' }],
    }),
  }),
})

export const { usePlatformAnalyticsQuery } = analyticsApi
