import type { DashboardMetrics } from '@/features/admin/dashboard/types'

// Placeholder metrics used until the admin analytics endpoints exist. Kept
// isolated so wiring the real feed (RTK Query) is a one-line change in the
// dashboard page.
export function mockDashboardMetrics(): DashboardMetrics {
  return {
    currency: 'AZN',
    dailyRevenueMinor: 428_650,
    tipsCollectedMinor: 51_200,
    averageTableMinutes: 47,
    payriffSuccessRate: 0.973,
    revenueSeries: [
      { label: '10', revenueMinor: 12_400 },
      { label: '11', revenueMinor: 18_900 },
      { label: '12', revenueMinor: 46_300 },
      { label: '13', revenueMinor: 61_800 },
      { label: '14', revenueMinor: 39_500 },
      { label: '15', revenueMinor: 27_100 },
      { label: '16', revenueMinor: 22_800 },
      { label: '17', revenueMinor: 34_600 },
      { label: '18', revenueMinor: 58_200 },
      { label: '19', revenueMinor: 71_400 },
      { label: '20', revenueMinor: 68_900 },
      { label: '21', revenueMinor: 46_750 },
    ],
  }
}
