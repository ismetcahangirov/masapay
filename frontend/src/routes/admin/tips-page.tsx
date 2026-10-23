import { useState } from 'react'
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

const PRESETS: { value: RangePreset; label: string }[] = [
  { value: 'today', label: 'Bu gün' },
  { value: 'week', label: 'Bu həftə' },
  { value: 'month', label: 'Bu ay' },
]

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
      saveFile(csv, `bexsisler-${range.from}-${range.to}.csv`)
    } catch {
      // csvState.error is shown below.
    }
  }

  const data = report.data
  const currency = data?.currency ?? 'AZN'

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">Bəxşişlər</h1>
          <p className="text-sm text-muted-foreground">
            Seçilmiş tarix aralığında ofisiantlar üzrə toplanan bəxşişlər
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            onClick={() => void onExcel()}
            disabled={!data || excelState.isLoading}
          >
            <FileSpreadsheet aria-hidden />
            {excelState.isLoading ? 'Hazırlanır...' : 'Excel yüklə'}
          </Button>
          <Button
            variant="outline"
            onClick={() => void onExport()}
            disabled={!data || csvState.isLoading}
          >
            <FileDown aria-hidden />
            {csvState.isLoading ? 'Hazırlanır...' : 'CSV yüklə'}
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
          Hesabınıza hələ restoran təyin olunmayıb.
        </p>
      )}

      <div className="flex flex-wrap items-end gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="tips-from">Başlanğıc</Label>
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
          <Label htmlFor="tips-to">Son</Label>
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
          aria-label="Tez seçim"
        >
          {PRESETS.map((preset) => (
            <ToggleGroupItem key={preset.value} value={preset.value}>
              {preset.label}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </div>

      {!complete && (
        <p className="text-sm text-muted-foreground">
          Hesabat üçün hər iki tarixi seçin.
        </p>
      )}
      {(restaurants.isLoading || report.isFetching) && !data && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label="Yüklənir"
        />
      )}
      {restaurants.isError && (
        <p className="text-sm text-destructive">
          Restoranları yükləmək alınmadı.
        </p>
      )}
      {report.isError && (
        <p className="text-sm text-destructive">
          {isBadRequest(report.error)
            ? 'Başlanğıc tarixi son tarixdən sonra ola bilməz.'
            : 'Bəxşiş hesabatını yükləmək alınmadı.'}
        </p>
      )}
      {csvState.isError && (
        <p className="text-sm text-destructive">CSV yükləmək alınmadı.</p>
      )}
      {excelState.isError && (
        <p className="text-sm text-destructive">Excel yükləmək alınmadı.</p>
      )}

      {data && !report.isError && (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            <KpiCard
              label="Ümumi bəxşiş"
              value={formatCurrency(data.totalTipAmount, currency)}
              icon={HandCoins}
              hint={`${data.from} - ${data.to}`}
            />
            <KpiCard
              label="Bəxşiş sayı"
              value={String(data.totalTipCount)}
              icon={Receipt}
              hint="Bəxşişli ödənişlər"
            />
          </div>

          {data.waiters.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Bu tarix aralığında bəxşiş yoxdur.
            </p>
          ) : (
            <>
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Ofisiantlar üzrə</CardTitle>
                  <CardDescription>
                    Hər ofisiantın topladığı bəxşiş
                  </CardDescription>
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
                        <th className="px-4 py-3 font-medium">Ofisiant</th>
                        <th className="px-4 py-3 font-medium">E-poçt</th>
                        <th className="px-4 py-3 text-right font-medium">
                          Say
                        </th>
                        <th className="px-4 py-3 text-right font-medium">
                          Cəm
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
