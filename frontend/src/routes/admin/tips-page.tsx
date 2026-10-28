import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  FileDown,
  FileSpreadsheet,
  HandCoins,
  LoaderCircle,
  Receipt,
} from 'lucide-react'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import { saveFile } from '@/features/admin/tables/tables-api'
import {
  useTipReportCsvMutation,
  useTipReportExcelMutation,
  useTipReportQuery,
} from '@/features/admin/tips/tips-api'
import {
  type DateRange,
  matchingPreset,
  presetRange,
  type RangePreset,
  waiterLabel,
} from '@/features/admin/tips/date-range'
import { TipsChart } from '@/features/admin/tips/tips-chart'
import { KpiCard } from '@/features/admin/dashboard/kpi-card'
import { formatCurrency } from '@/features/order/format'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

const PRESETS: RangePreset[] = ['today', 'week', 'month']

function isBadRequest(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    'status' in error &&
    error.status === 400
  )
}

// Tip report (#48): tips collected per waiter in a date range, with an Excel
// (#170) and a CSV download. A manager sees their own restaurant; a super
// admin picks one.
export function TipsPage() {
  const { t } = useTranslation('tips')
  const restaurants = useMyRestaurantsQuery()
  const [chosen, setChosen] = useState<string | null>(null)
  const restaurantId = chosen ?? restaurants.data?.[0]?.id ?? null

  const [range, setRange] = useState<DateRange>(() => presetRange('month'))
  const complete = Boolean(range.from && range.to)

  const report = useTipReportQuery(
    { restaurantId: restaurantId ?? '', ...range },
    { skip: !restaurantId || !complete },
  )
  const [downloadCsv, csvState] = useTipReportCsvMutation()
  const [downloadExcel, excelState] = useTipReportExcelMutation()

  async function onExcel() {
    if (!restaurantId) return
    try {
      const file = await downloadExcel({ restaurantId, ...range }).unwrap()
      saveFile(file.blob, file.filename)
    } catch {
      // excelState.error is shown below.
    }
  }

  async function onExport() {
    if (!restaurantId) return
    try {
      const csv = await downloadCsv({ restaurantId, ...range }).unwrap()
      saveFile(
        csv,
        `${t('files.report', { from: range.from, to: range.to })}.csv`,
      )
    } catch {
      // csvState.error is shown below.
    }
  }

  const data = report.data
  const currency = data?.currency ?? 'AZN'

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-4 sm:p-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">
            {t('report.title')}
          </h1>
          <p className="text-sm text-muted-foreground">
            {t('report.subtitle')}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            onClick={() => void onExcel()}
            disabled={!data || excelState.isLoading}
          >
            <FileSpreadsheet aria-hidden />
            {excelState.isLoading
              ? t('report.preparing')
              : t('report.downloadExcel')}
          </Button>
          <Button
            variant="outline"
            onClick={() => void onExport()}
            disabled={!data || csvState.isLoading}
          >
            <FileDown aria-hidden />
            {csvState.isLoading
              ? t('report.preparing')
              : t('report.downloadCsv')}
          </Button>
        </div>
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
        <p className="text-sm text-muted-foreground">
          {t('report.noRestaurant')}
        </p>
      )}

      <div className="flex flex-wrap items-end gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="tips-from">{t('report.from')}</Label>
          <Input
            id="tips-from"
            type="date"
            value={range.from}
            max={range.to || undefined}
            onChange={(event) =>
              setRange((r) => ({ ...r, from: event.target.value }))
            }
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="tips-to">{t('report.to')}</Label>
          <Input
            id="tips-to"
            type="date"
            value={range.to}
            min={range.from || undefined}
            onChange={(event) =>
              setRange((r) => ({ ...r, to: event.target.value }))
            }
          />
        </div>
        <ToggleGroup
          type="single"
          value={matchingPreset(range) ?? ''}
          onValueChange={(value) =>
            value && setRange(presetRange(value as RangePreset))
          }
          className="justify-start"
          aria-label={t('report.presets')}
        >
          {PRESETS.map((preset) => (
            <ToggleGroupItem key={preset} value={preset}>
              {t(`report.${preset}`)}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </div>

      {!complete && (
        <p className="text-sm text-muted-foreground">{t('report.pickBoth')}</p>
      )}
      {(restaurants.isLoading || report.isFetching) && !data && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label={t('report.loading')}
        />
      )}
      {restaurants.isError && (
        <p className="text-sm text-destructive">
          {t('report.restaurantsFailed')}
        </p>
      )}
      {report.isError && (
        <p className="text-sm text-destructive">
          {isBadRequest(report.error)
            ? t('report.badRange')
            : t('report.loadFailed')}
        </p>
      )}
      {csvState.isError && (
        <p className="text-sm text-destructive">{t('report.csvFailed')}</p>
      )}
      {excelState.isError && (
        <p className="text-sm text-destructive">{t('report.excelFailed')}</p>
      )}

      {data && !report.isError && (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            <KpiCard
              label={t('report.totalTips')}
              value={formatCurrency(data.totalTipAmount, currency)}
              icon={HandCoins}
              hint={`${data.from} - ${data.to}`}
            />
            <KpiCard
              label={t('report.tipCount')}
              value={String(data.totalTipCount)}
              icon={Receipt}
              hint={t('report.tipCountHint')}
            />
          </div>

          {data.waiters.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t('report.empty')}</p>
          ) : (
            <>
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">
                    {t('report.byWaiter')}
                  </CardTitle>
                  <CardDescription>{t('report.byWaiterHint')}</CardDescription>
                </CardHeader>
                <CardContent>
                  <TipsChart waiters={data.waiters} currency={currency} />
                </CardContent>
              </Card>

              <Card>
                <CardContent className="overflow-x-auto p-0">
                  <table className="w-full text-sm">
                    <thead className="border-b text-left text-muted-foreground">
                      <tr>
                        <th className="px-4 py-3 font-medium">
                          {t('report.waiter')}
                        </th>
                        <th className="px-4 py-3 font-medium">
                          {t('report.email')}
                        </th>
                        <th className="px-4 py-3 text-right font-medium">
                          {t('report.count')}
                        </th>
                        <th className="px-4 py-3 text-right font-medium">
                          {t('report.sum')}
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.waiters.map((waiter) => (
                        <tr
                          key={waiter.waiterId ?? 'unassigned'}
                          className="border-b last:border-0"
                        >
                          <td className="px-4 py-3 font-medium">
                            {waiterLabel(waiter)}
                          </td>
                          <td className="px-4 py-3 text-muted-foreground">
                            {waiter.waiterEmail ?? '-'}
                          </td>
                          <td className="px-4 py-3 text-right tabular-nums">
                            {waiter.tipCount}
                          </td>
                          <td className="px-4 py-3 text-right font-medium tabular-nums">
                            {formatCurrency(waiter.tipTotal, currency)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </CardContent>
              </Card>
            </>
          )}
        </>
      )}
    </div>
  )
}
