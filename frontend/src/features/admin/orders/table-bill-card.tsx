import { useEffect, useRef, useState } from 'react'
import { BellRing, CircleCheck, Info } from 'lucide-react'
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
import { KitchenStatusBadge } from '@/features/order/kitchen-status-badge'
import { OrderStatusBadge } from '@/features/order/order-status-badge'
import { orderBalance, type Order } from '@/features/order/types'
import { cn } from '@/lib/utils'

/** How long a dish that just turned ready stays highlighted. */
export const JUST_READY_MS = 2 * 60 * 1000

/**
 * Ids of dishes that turned READY while the screen was open (#178), each kept
 * for {@link JUST_READY_MS}, so the waiter sees what to take to the table now.
 * Dishes already ready when the screen opened are not highlighted.
 */
function useJustReady(order: Order): Set<string> {
  const previous = useRef<Map<string, string | null> | null>(null)
  const [justReady, setJustReady] = useState<Set<string>>(new Set())

  useEffect(() => {
    const before = previous.current
    previous.current = new Map(order.items.map((i) => [i.id, i.kitchenStatus]))
    if (!before) return
    const turned = order.items
      .filter(
        (i) => i.kitchenStatus === 'READY' && before.get(i.id) !== 'READY',
      )
      .map((i) => i.id)
    if (turned.length === 0) return
    setJustReady((current) => new Set([...current, ...turned]))
    const timer = setTimeout(
      () =>
        setJustReady((current) => {
          const next = new Set(current)
          turned.forEach((id) => next.delete(id))
          return next
        }),
      JUST_READY_MS,
    )
    return () => clearTimeout(timer)
  }, [order.items])

  return justReady
}

// One table on the staff orders screen (#143): what was ordered, where each
// dish is in the kitchen, which lines are paid, and what is still to pay.
// Ready dishes that are not paid yet are counted in the header, and a dish
// that just turned ready is highlighted (#178).
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
  const justReady = useJustReady(order)
  const readyCount = order.items.filter(
    (item) => item.kitchenStatus === 'READY' && !item.paid,
  ).length
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
        <div className="flex flex-wrap items-center justify-end gap-2">
          {readyCount > 0 && (
            <Badge variant="success" className="gap-1">
              <BellRing className="size-3" aria-hidden />
              {readyCount} yemək hazırdır
            </Badge>
          )}
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
                data-just-ready={justReady.has(item.id) || undefined}
                className={cn(
                  'flex items-start justify-between gap-3 py-2',
                  justReady.has(item.id) &&
                    '-mx-2 rounded-md bg-success/15 px-2',
                )}
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
                      <KitchenStatusBadge status={item.kitchenStatus} />
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
