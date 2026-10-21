import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { RevenuePoint } from '@/features/admin/dashboard/types'
import { formatCurrency } from '@/features/order/format'

// Solid palette colours only (no gradients), pulled from the design tokens so
// the chart follows the theme.
const BAR_COLOR = 'hsl(var(--success))'
const GRID_COLOR = 'hsl(var(--border))'
const AXIS_COLOR = 'hsl(var(--muted-foreground))'

const DAY_FORMAT = new Intl.DateTimeFormat('az-AZ', {
  day: 'numeric',
  month: 'long',
  weekday: 'short',
  timeZone: 'UTC',
})

/** "2026-10-21" to a readable day with weekday and month name. */
function formatDay(isoDate: string | undefined): string | undefined {
  return isoDate
    ? DAY_FORMAT.format(new Date(`${isoDate}T00:00:00Z`))
    : undefined
}

export function RevenueChart({
  data,
  currency,
}: {
  data: RevenuePoint[]
  currency: string
}) {
  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 8 }}>
        <CartesianGrid vertical={false} stroke={GRID_COLOR} />
        <XAxis
          dataKey="label"
          tickLine={false}
          axisLine={false}
          stroke={AXIS_COLOR}
          fontSize={12}
        />
        <YAxis
          tickLine={false}
          axisLine={false}
          stroke={AXIS_COLOR}
          fontSize={12}
          width={64}
          tickFormatter={(value: number) => formatCurrency(value, currency)}
        />
        <Tooltip
          cursor={{ fill: 'hsl(var(--muted))' }}
          formatter={(value: number) => [
            formatCurrency(value, currency),
            'Dövriyyə',
          ]}
          labelFormatter={(label: string, payload) =>
            formatDay(
              (payload?.[0]?.payload as RevenuePoint | undefined)?.date,
            ) ?? label
          }
          contentStyle={{
            borderRadius: 8,
            border: '1px solid hsl(var(--border))',
            background: 'hsl(var(--popover))',
            color: 'hsl(var(--popover-foreground))',
            fontSize: 12,
          }}
        />
        <Bar dataKey="revenueMinor" fill={BAR_COLOR} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}
