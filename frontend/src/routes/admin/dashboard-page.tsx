import { useState } from 'react'
import {
  Banknote,
  HandCoins,
  Timer,
  CircleCheckBig,
  LoaderCircle,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { formatCurrency } from '@/features/order/format'
import i18n from '@/i18n'
import { KpiCard } from '@/features/admin/dashboard/kpi-card'
import { RevenueChart } from '@/features/admin/dashboard/revenue-chart'
import { useDashboardMetricsQuery } from '@/features/admin/dashboard/dashboard-api'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

const SERIES_DAYS = 7
// The KPIs follow live payments; refetch once a minute while the page is open.
const POLLING_MS = 60_000

function formatMinutes(minutes: number | null): string {
  if (minutes === null) return '-'
  const hours = Math.floor(minutes / 60)
  const rest = Math.round(minutes % 60)
  return hours > 0
    ? i18n.t('dashboard:duration.hoursMinutes', { hours, minutes: rest })
    : i18n.t('dashboard:duration.minutes', { minutes: rest })
}

function formatPercent(ratio: number | null): string {
  return ratio === null ? '-' : `${(ratio * 100).toFixed(1)}%`
}

// Admin dashboard (#43): today's KPIs and the last days' revenue of a
// restaurant, from the backend. A manager sees their own restaurant; a super
// admin picks one.
export function DashboardPage() {
  const { t } = useTranslation('dashboard')
  const restaurants = useMyRestaurantsQuery()
  const [chosen, setChosen] = useState<string | null>(null)
  const restaurantId = chosen ?? restaurants.data?.[0]?.id ?? null

  const dashboard = useDashboardMetricsQuery(
    { restaurantId: restaurantId ?? '', days: SERIES_DAYS },
    { skip: !restaurantId, pollingInterval: POLLING_MS },
  )
  const metrics = dashboard.data

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-4 sm:p-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">{t('title')}</h1>
        <p className="text-sm text-muted-foreground">{t('subtitle')}</p>
      </header>

      {restaurants.data && restaurants.data.length > 1 && (
        <ToggleGroup
          type="single"
          value={restaurantId ?? undefined}
          onValueChange={(value) => value && setChosen(value)}
          className="flex-wrap justify-start"
        >
          {restaurants.data.map((restaurant) => (
            <ToggleGroupItem key={restaurant.id} value={restaurant.id}>
              {restaurant.name}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      )}

      {restaurants.data?.length === 0 && (
        <p className="text-sm text-muted-foreground">{t('noRestaurant')}</p>
      )}

      {(restaurants.isLoading || dashboard.isFetching) && !metrics && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label={t('loading')}
        />
      )}
      {(restaurants.isError || dashboard.isError) && (
        <p className="text-sm text-destructive">{t('loadFailed')}</p>
      )}

      {metrics && (
        <>
          <div className="grid grid-cols-[repeat(auto-fill,minmax(13rem,1fr))] gap-4">
            <KpiCard
              label={t('kpi.dailyRevenue')}
              value={formatCurrency(
                metrics.dailyRevenueMinor,
                metrics.currency,
              )}
              hint={t('kpi.tipsExcluded')}
              icon={Banknote}
            />
            <KpiCard
              label={t('kpi.tipsCollected')}
              value={formatCurrency(
                metrics.tipsCollectedMinor,
                metrics.currency,
              )}
              icon={HandCoins}
            />
            <KpiCard
              label={t('kpi.averageTableTime')}
              value={formatMinutes(metrics.averageTableMinutes)}
              hint={
                metrics.averageTableMinutes === null
                  ? t('kpi.noPaidBills')
                  : undefined
              }
              icon={Timer}
            />
            <KpiCard
              label={t('kpi.payriffSuccessRate')}
              value={formatPercent(metrics.payriffSuccessRate)}
              hint={
                metrics.payriffSuccessRate === null
                  ? t('kpi.noPayriffPayments')
                  : undefined
              }
              icon={CircleCheckBig}
            />
          </div>

          <Card>
            <CardHeader>
              <CardTitle>{t('chart.title')}</CardTitle>
              <CardDescription>
                {t('chart.description', { count: SERIES_DAYS })}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <RevenueChart
                data={metrics.revenueSeries}
                currency={metrics.currency}
              />
            </CardContent>
          </Card>
        </>
      )}
    </div>
  )
}
