import { CircleCheck, Info } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { formatCurrency } from '@/features/order/format'
import { OrderStatusBadge } from '@/features/order/order-status-badge'
import {
  orderBalance,
  type KitchenStatus,
  type Order,
} from '@/features/order/types'
import { cn } from '@/lib/utils'

const KITCHEN_LABELS: Record<KitchenStatus, string> = {
  NEW: 'Mətbəxdə: yeni',
  PREPARING: 'Hazırlanır',
  READY: 'Hazırdır',
}

// One table on the staff orders screen (#143): what was ordered, where each
// dish is in the kitchen, which lines are paid, and what is still to pay.
export function TableBillCard({
  tableLabel,
  order,
  onOpenDetail,
}: {
  tableLabel: string
  order: Order
  /** Opens the table's detail: who added what, payments, adding a dish (#144). */
  onOpenDetail?: () => void
}) {
  const left = orderBalance(order)
  const progress =
    order.totalAmount > 0
      ? Math.min(100, Math.round((order.paidAmount / order.totalAmount) * 100))
      : 0

  return (
    <Card role="region" aria-label={tableLabel}>
      <CardHeader className="flex-row items-center justify-between gap-2 space-y-0 pb-3">
        <CardTitle className="text-lg">
          <h2>{tableLabel}</h2>
        </CardTitle>
        <div className="flex items-center gap-2">
          <OrderStatusBadge status={order.status} />
          {onOpenDetail && (
            <Button
              variant="outline"
              size="sm"
              onClick={onOpenDetail}
              aria-label={`${tableLabel} ətraflı`}
            >
              <Info aria-hidden />
              Ətraflı
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent>
        {order.items.length === 0 ? (
          <p className="text-sm text-muted-foreground">Hələ məhsul yoxdur.</p>
        ) : (
          <ul className="divide-y text-sm">
            {order.items.map((item) => (
              <li
                key={item.id}
                className="flex items-start justify-between gap-3 py-2"
              >
                <div className="min-w-0 space-y-1">
                  <p
                    className={cn(
                      item.paid && 'text-muted-foreground line-through',
                    )}
                  >
                    {item.quantity} x {item.name}
                  </p>
                  <div className="flex flex-wrap gap-1">
                    {item.kitchenStatus && (
                      <Badge
                        variant={
                          item.kitchenStatus === 'READY' ? 'success' : 'outline'
                        }
                      >
                        {KITCHEN_LABELS[item.kitchenStatus]}
                      </Badge>
                    )}
                    {item.paid && (
                      <Badge variant="secondary">
                        <CircleCheck className="mr-1 size-3" aria-hidden />
                        Ödənilib
                      </Badge>
                    )}
                  </div>
                </div>
                <span className="shrink-0 tabular-nums">
                  {formatCurrency(item.total, order.currency)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
      <CardFooter className="flex-col items-stretch gap-2 border-t pt-4 text-sm">
        <div
          className="h-2 w-full overflow-hidden rounded-full bg-muted"
          role="progressbar"
          aria-label="Ödənilib"
          aria-valuenow={progress}
          aria-valuemin={0}
          aria-valuemax={100}
        >
          <div
            className="h-full rounded-full bg-success"
            style={{ width: `${progress}%` }}
          />
        </div>
        <dl className="grid grid-cols-3 gap-2 tabular-nums">
          <div>
            <dt className="text-xs text-muted-foreground">Cəmi</dt>
            <dd className="font-medium">
              {formatCurrency(order.totalAmount, order.currency)}
            </dd>
          </div>
          <div>
            <dt className="text-xs text-muted-foreground">Ödənilib</dt>
            <dd className="font-medium">
              {formatCurrency(order.paidAmount, order.currency)}
            </dd>
          </div>
          <div>
            <dt className="text-xs text-muted-foreground">Qalıb</dt>
            <dd className="font-semibold">
              {formatCurrency(left, order.currency)}
            </dd>
          </div>
        </dl>
      </CardFooter>
    </Card>
  )
}
