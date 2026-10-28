import { useState } from 'react'
import {
  Banknote,
  Building2,
  CalendarClock,
  ChartColumn,
  LoaderCircle,
  ReceiptText,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { KpiCard } from '@/features/admin/dashboard/kpi-card'
import {
  type CurrencyTotals,
  usePlatformAnalyticsQuery,
} from '@/features/admin/platform/analytics-api'
import {
  STATUS_LABELS,
  bakuToday,
  formatDay,
} from '@/features/admin/platform/subscription'
import { formatCurrency } from '@/features/order/format'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

const PRESETS = [7, 30, 90] as const
const DAY = /^\d{4}-\d{2}-\d{2}$/

/** The day `days - 1` days before `day`, so the range holds `days` days. */
function daysBack(day: string, days: number): string {
  const date = new Date(`${day}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() - (days - 1))
  return date.toISOString().slice(0, 10)
}

// Platform analytics (#149), for the super admin: turnover, payments and
// subscription income of every restaurant over a range of days, with totals
// per currency and a table to compare the restaurants.
export function PlatformAnalyticsPage() {
  const { t } = useTranslation('platform')
  const today = bakuToday()
  const [to, setTo] = useState(today)
  const [from, setFrom] = useState(daysBack(today, 30))
  const [preset, setPreset] = useState('30')

  const valid = DAY.test(from) && DAY.test(to) && from <= to
  const analytics = usePlatformAnalyticsQuery({ from, to }, { skip: !valid })
  const data = analytics.data

  function choosePreset(value: string) {
    if (!value) return
    setPreset(value)
    setTo(today)
    setFrom(daysBack(today, Number(value)))
  }

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-4 sm:p-6">
      <header className="space-y-1">
        <h1 className="flex items-center gap-2 text-2xl font-semibold tracking-tight">
          <ChartColumn className="size-6" aria-hidden />
          {t('analytics.title')}
        </h1>
        <p className="text-sm text-muted-foreground">
          {t('analytics.description')}
        </p>
      </header>

      <div className="flex flex-wrap items-end gap-4">
        <ToggleGroup
          type="single"
          value={preset}
          onValueChange={choosePreset}
          aria-label={t('analytics.period')}
        >
          {PRESETS.map((days) => (
            <ToggleGroupItem key={days} value={String(days)}>
              {t('analytics.lastDays', { count: days })}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
        <div className="space-y-1">
          <Label htmlFor="analytics-from">{t('analytics.from')}</Label>
          <Input
            id="analytics-from"
            type="date"
            value={from}
            max={to}
            onChange={(event) => {
              setFrom(event.target.value)
              setPreset('')
            }}
          />
        </div>
        <div className="space-y-1">
          <Label htmlFor="analytics-to">{t('analytics.to')}</Label>
          <Input
            id="analytics-to"
            type="date"
            value={to}
            min={from}
            onChange={(event) => {
              setTo(event.target.value)
              setPreset('')
            }}
          />
        </div>
      </div>
      {!valid && (
        <p className="text-sm text-destructive">{t('analytics.backwards')}</p>
      )}

      {analytics.isFetching && !data && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label={t('analytics.loading')}
        />
      )}
      {analytics.isError && (
        <p className="text-sm text-destructive">{t('analytics.loadFailed')}</p>
      )}

      {data && (
        <>
          <p className="text-sm text-muted-foreground">
            {formatDay(data.from)} - {formatDay(data.to)}
          </p>
          {data.totals.map((totals) => (
            <CurrencyKpis key={totals.currency} totals={totals} />
          ))}

          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('analytics.columns.restaurant')}</TableHead>
                  <TableHead>{t('analytics.columns.subscription')}</TableHead>
                  <TableHead className="text-right">
                    {t('analytics.columns.payments')}
                  </TableHead>
                  <TableHead className="text-right">
                    {t('analytics.columns.revenue')}
                  </TableHead>
                  <TableHead className="text-right">
                    {t('analytics.columns.averagePayment')}
                  </TableHead>
                  <TableHead className="text-right">
                    {t('analytics.columns.tips')}
                  </TableHead>
                  <TableHead className="text-right">
                    {t('analytics.columns.subscriptionRevenue')}
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.restaurants.map((row) => (
                  <TableRow key={row.id}>
                    <TableCell>
                      <p className="font-medium">{row.name}</p>
                      {!row.active && (
                        <p className="text-xs text-muted-foreground">
                          {t('analytics.inactive')}
                        </p>
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant={
                          row.subscriptionStatus === 'EXPIRED'
                            ? 'destructive'
                            : 'outline'
                        }
                      >
                        {t(STATUS_LABELS[row.subscriptionStatus])}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {row.paymentCount}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatCurrency(row.revenueMinor, row.currency)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {row.averagePaymentMinor === null
                        ? '-'
                        : formatCurrency(row.averagePaymentMinor, row.currency)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatCurrency(row.tipsMinor, row.currency)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatCurrency(
                        row.subscriptionRevenueMinor,
                        row.currency,
                      )}
                    </TableCell>
                  </TableRow>
                ))}
                {data.restaurants.length === 0 && (
                  <TableRow>
                    <TableCell
                      colSpan={7}
                      className="py-10 text-center text-muted-foreground"
                    >
                      {t('analytics.empty')}
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </>
      )}
    </div>
  )
}

function CurrencyKpis({ totals }: { totals: CurrencyTotals }) {
  const { t } = useTranslation('platform')
  return (
    <section
      aria-label={t('analytics.totals', { currency: totals.currency })}
      className="space-y-2"
    >
      <div className="grid grid-cols-[repeat(auto-fill,minmax(13rem,1fr))] gap-4">
        <KpiCard
          label={t('analytics.revenue', { currency: totals.currency })}
          value={formatCurrency(totals.revenueMinor, totals.currency)}
          hint={t('analytics.tipsApart', {
            amount: formatCurrency(totals.tipsMinor, totals.currency),
          })}
          icon={Banknote}
        />
        <KpiCard
          label={t('analytics.paymentCount')}
          value={String(totals.paymentCount)}
          icon={ReceiptText}
        />
        <KpiCard
          label={t('analytics.activeRestaurants')}
          value={`${totals.activeRestaurantCount} / ${totals.restaurantCount}`}
          icon={Building2}
        />
        <KpiCard
          label={t('analytics.subscriptionRevenue')}
          value={formatCurrency(
            totals.subscriptionRevenueMinor,
            totals.currency,
          )}
          hint={t('analytics.subscriptionRevenueHint')}
          icon={CalendarClock}
        />
      </div>
    </section>
  )
}
