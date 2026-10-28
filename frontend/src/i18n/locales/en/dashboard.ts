import type { Messages } from '@/i18n/messages'
import type az from '../az/dashboard'

const en: Messages<typeof az> = {
  title: 'Dashboard',
  subtitle: "Today's figures",
  noRestaurant: 'No restaurant has been assigned to your account yet.',
  loading: 'Loading',
  loadFailed: 'Could not load the figures.',
  kpi: {
    dailyRevenue: 'Revenue today',
    tipsExcluded: 'Tips not included',
    tipsCollected: 'Tips collected',
    averageTableTime: 'Average table time',
    noPaidBills: 'No bills paid today',
    payriffSuccessRate: 'Payriff success rate',
    noPayriffPayments: 'No Payriff payments today',
  },
  duration: {
    hoursMinutes: '{{hours}} h {{minutes}} min',
    minutes: '{{minutes}} min',
  },
  chart: {
    title: 'Revenue (daily)',
    description_one: 'Last {{count}} day, revenue per day',
    description_other: 'Last {{count}} days, revenue per day',
    revenue: 'Revenue',
  },
}

export default en
