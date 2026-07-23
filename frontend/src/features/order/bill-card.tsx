import { type Order, orderTotal, orderBalance } from '@/features/order/types'
import { formatCurrency } from '@/features/order/format'
import { OrderStatusBadge } from '@/features/order/order-status-badge'
import { OrderItemList } from '@/features/order/order-item-list'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

function TotalRow({
  label,
  amount,
  currency,
  emphasis = false,
}: {
  label: string
  amount: number
  currency: string
  emphasis?: boolean
}) {
  return (
    <div className="flex items-center justify-between">
      <span
        className={
          emphasis ? 'text-sm font-semibold' : 'text-sm text-muted-foreground'
        }
      >
        {label}
      </span>
      <span
        className={
          emphasis
            ? 'tabular-nums text-lg font-semibold'
            : 'tabular-nums text-sm'
        }
      >
        {formatCurrency(amount, currency)}
      </span>
    </div>
  )
}

// Presentational live-bill card built from shadcn Card + Badge. Fed with mock
// data today; wired to the RTK Query order feed in sub-issue 4.2.
export function BillCard({ order }: { order: Order }) {
  const total = orderTotal(order)
  const balance = orderBalance(order)

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <div className="flex items-center justify-between gap-3">
          <CardTitle>Masa {order.tableId}</CardTitle>
          <OrderStatusBadge status={order.status} />
        </div>
        <CardDescription>{order.restaurantSlug}</CardDescription>
      </CardHeader>

      <CardContent>
        <OrderItemList items={order.items} currency={order.currency} />
      </CardContent>

      <CardFooter className="flex-col items-stretch gap-2 border-t pt-6">
        <TotalRow label="Cəmi" amount={total} currency={order.currency} />
        {order.paidAmount > 0 && (
          <TotalRow
            label="Ödənilib"
            amount={order.paidAmount}
            currency={order.currency}
          />
        )}
        <TotalRow
          label="Qalıq"
          amount={balance}
          currency={order.currency}
          emphasis
        />
      </CardFooter>
    </Card>
  )
}
