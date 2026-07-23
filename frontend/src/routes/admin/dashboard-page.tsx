import { useMemo } from 'react'
import { Banknote, HandCoins, Timer, CircleCheckBig } from 'lucide-react'
import { formatCurrency } from '@/features/order/format'
import { KpiCard } from '@/features/admin/dashboard/kpi-card'
import { RevenueChart } from '@/features/admin/dashboard/revenue-chart'
import { mockDashboardMetrics } from '@/features/admin/dashboard/mock'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

function formatMinutes(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = Math.round(minutes % 60)
  return hours > 0 ? `${hours} sa ${rest} dəq` : `${rest} dəq`
}

function formatPercent(ratio: number): string {
  return `${(ratio * 100).toFixed(1)}%`
}

export function DashboardPage() {
  // Swapped for the admin analytics RTK Query feed once the backend lands.
  const metrics = useMemo(() => mockDashboardMetrics(), [])

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
        <p className="text-sm text-muted-foreground">Bugünkü göstəricilər</p>
      </header>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard
          label="Günlük dövriyyə"
          value={formatCurrency(metrics.dailyRevenueMinor, metrics.currency)}
          icon={Banknote}
        />
        <KpiCard
          label="Toplanan bəxşiş"
          value={formatCurrency(metrics.tipsCollectedMinor, metrics.currency)}
          icon={HandCoins}
        />
        <KpiCard
          label="Orta masa vaxtı"
          value={formatMinutes(metrics.averageTableMinutes)}
          icon={Timer}
        />
        <KpiCard
          label="Payriff uğur faizi"
          value={formatPercent(metrics.payriffSuccessRate)}
          icon={CircleCheckBig}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Dövriyyə (saatlıq)</CardTitle>
          <CardDescription>Bugün, saat üzrə dövriyyə</CardDescription>
        </CardHeader>
        <CardContent>
          <RevenueChart
            data={metrics.revenueSeries}
            currency={metrics.currency}
          />
        </CardContent>
      </Card>
    </div>
  )
}
