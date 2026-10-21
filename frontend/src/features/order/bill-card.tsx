import {
  type TableOrder,
  orderTotal,
  orderBalance,
} from '@/features/order/types'
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

// Presentational live-bill card built from shadcn Card + Badge, fed by the RTK
// Query order feed (#20). Only rendered when the table has an open bill.
export function BillCard({
  table,
  order,
}: {
  table: TableOrder
  order: NonNullable<TableOrder['order']>
}) {
  const total = orderTotal(order)
  const balance = orderBalance(order)

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <div className="flex items-center justify-between gap-3">
          <CardTitle>{table.tableLabel}</CardTitle>
          <OrderStatusBadge status={order.status} />
        </div>
        <CardDescription>{table.restaurantName}</CardDescription>
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
