import { CircleCheckBig } from 'lucide-react'
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
import { cn } from '@/lib/utils'

function TotalRow({
  label,
  amount,
  currency,
  tone = 'muted',
}: {
  label: string
  amount: number
  currency: string
  tone?: 'muted' | 'paid' | 'emphasis'
}) {
  return (
    <div className="flex items-center justify-between">
      <span
        className={cn(
          'text-sm',
          tone === 'muted' && 'text-muted-foreground',
          tone === 'paid' && 'text-success',
          tone === 'emphasis' && 'font-semibold',
        )}
      >
        {label}
      </span>
      <span
        className={cn(
          'tabular-nums',
          tone === 'emphasis' ? 'text-lg font-semibold' : 'text-sm',
          tone === 'paid' && 'text-success',
        )}
      >
        {formatCurrency(amount, currency)}
      </span>
    </div>
  )
}

/** Thin bar showing how much of the bill is paid. */
function PaidProgress({ paid, total }: { paid: number; total: number }) {
  const percent = total > 0 ? Math.min(100, (paid / total) * 100) : 0
  return (
    <div
      className="h-2 w-full overflow-hidden rounded-full bg-muted"
      role="progressbar"
      aria-label="Ödənilmiş hissə"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(percent)}
    >
      <div
        className="h-full rounded-full bg-success transition-[width] duration-500"
        style={{ width: `${percent}%` }}
      />
    </div>
  )
}

// The live bill as a receipt (#20, #24-#26): every line stays visible, paid
// lines are crossed out, and the footer shows paid vs. remaining. A fully paid
// bill keeps showing for a while with a "paid" banner instead of disappearing.
export function BillCard({
  table,
  order,
}: {
  table: TableOrder
  order: NonNullable<TableOrder['order']>
}) {
  const total = orderTotal(order)
  const balance = orderBalance(order)
  const fullyPaid = order.status === 'paid' || balance === 0

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <div className="flex items-center justify-between gap-3">
          <CardTitle>{table.tableLabel}</CardTitle>
          <OrderStatusBadge status={order.status} />
        </div>
        <CardDescription>{table.restaurantName}</CardDescription>
      </CardHeader>

      {fullyPaid && (
        <div className="mx-6 mb-2 flex items-center gap-3 rounded-lg bg-success/10 px-4 py-3 text-success">
          <CircleCheckBig className="size-5 shrink-0" aria-hidden />
          <span className="text-sm font-medium">
            Hesab tam ödənilib. Təşəkkür edirik!
          </span>
        </div>
      )}

      <CardContent>
        <OrderItemList items={order.items} currency={order.currency} />
      </CardContent>

      <CardFooter className="flex-col items-stretch gap-2 border-t pt-6">
        {order.serviceCharge && (
          <>
            <TotalRow
              label="Məhsullar"
              amount={total - order.serviceCharge.amount}
              currency={order.currency}
            />
            <TotalRow
              label={
                table.serviceChargePercent
                  ? `Xidmət haqqı (${table.serviceChargePercent}%)`
                  : 'Xidmət haqqı'
              }
              amount={order.serviceCharge.amount}
              currency={order.currency}
            />
          </>
        )}
        <TotalRow
          label="Cəmi"
          amount={total}
          currency={order.currency}
          tone={order.serviceCharge ? 'emphasis' : 'muted'}
        />
        {order.paidAmount > 0 && (
          <>
            <TotalRow
              label="Ödənilib"
              amount={order.paidAmount}
              currency={order.currency}
              tone="paid"
            />
            <PaidProgress paid={order.paidAmount} total={total} />
          </>
        )}
        {order.equalSplit && !fullyPaid && (
          <p className="text-xs text-muted-foreground">
            Bərabər bölgü: {order.equalSplit.paidParts} /{' '}
            {order.equalSplit.totalParts} hissə ödənilib
          </p>
        )}
        <TotalRow
          label="Qalıq"
          amount={balance}
          currency={order.currency}
          tone="emphasis"
        />
      </CardFooter>
    </Card>
  )
}
