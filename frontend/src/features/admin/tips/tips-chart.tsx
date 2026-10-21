import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { WaiterTips } from '@/features/admin/tips/tips-api'
import { waiterLabel } from '@/features/admin/tips/date-range'
import { formatCurrency } from '@/features/order/format'

// Solid palette colours from the design tokens (no gradients), like the
// dashboard revenue chart.
const BAR_COLOR = 'hsl(var(--success))'
const GRID_COLOR = 'hsl(var(--border))'
const AXIS_COLOR = 'hsl(var(--muted-foreground))'

/** Tips collected per waiter, one bar each. */
export function TipsChart({
  waiters,
  currency,
}: {
  waiters: WaiterTips[]
  currency: string
}) {
  const data = waiters.map((waiter) => ({
    name: waiterLabel(waiter),
    tipTotal: waiter.tipTotal,
  }))

  return (
    <ResponsiveContainer width="100%" height={240}>
      <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 8 }}>
        <CartesianGrid vertical={false} stroke={GRID_COLOR} />
        <XAxis
          dataKey="name"
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
          width={72}
          tickFormatter={(value: number) => formatCurrency(value, currency)}
        />
        <Tooltip
          cursor={{ fill: 'hsl(var(--muted))' }}
          formatter={(value: number) => [
            formatCurrency(value, currency),
            'Bəxşiş',
          ]}
          contentStyle={{
            borderRadius: 8,
            border: '1px solid hsl(var(--border))',
            background: 'hsl(var(--popover))',
            color: 'hsl(var(--popover-foreground))',
            fontSize: 12,
          }}
        />
        <Bar
          dataKey="tipTotal"
          fill={BAR_COLOR}
          radius={[4, 4, 0, 0]}
          maxBarSize={56}
        />
      </BarChart>
    </ResponsiveContainer>
  )
}
