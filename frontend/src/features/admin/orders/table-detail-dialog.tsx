import { useState } from 'react'
import { Minus, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useRestaurantMenuQuery } from '@/features/admin/menu/menu-api'
import {
  formatAmount,
  formatDateTime,
  SPLIT_LABELS,
} from '@/features/admin/transactions/format'
import { TransactionStatusBadge } from '@/features/admin/transactions/transaction-status'
import { formatCurrency } from '@/features/order/format'
import type { TableOrder } from '@/features/order/types'
import { cn } from '@/lib/utils'
import { useAppSelector } from '@/app/hooks'
import { CancelOrderSection } from './cancel-order-section'
import { lineAuthor } from './line-author'
import { useAddMenuDishMutation, useTablePaymentsQuery } from './orders-api'

const MAX_QUANTITY = 20

// The detail of one table's bill (#144): every line with who added it and
// when, the payments made on it, and adding a dish from today's menu. A
// manager can also cancel the whole order from here; a waiter can not (#186).
export function TableDetailDialog({
  restaurantId,
  table,
  onClose,
}: {
  restaurantId: string
  /** The table to show; null keeps the dialog closed. */
  table: TableOrder | null
  onClose: () => void
}) {
  const order = table?.order ?? null
  const role = useAppSelector((state) => state.auth.user?.role)
  const canCancel =
    (role === 'RESTO_MANAGER' || role === 'SUPER_ADMIN') &&
    order !== null &&
    order.status !== 'paid' &&
    order.status !== 'cancelled'
  const payments = useTablePaymentsQuery(table?.tableId ?? '', {
    skip: !table,
  })
  const menu = useRestaurantMenuQuery(restaurantId, { skip: !table })
  const [addDish, addState] = useAddMenuDishMutation()
  const [dishId, setDishId] = useState('')
  const [quantity, setQuantity] = useState(1)

  const todaysDishes = (menu.data ?? [])
    .flatMap((category) => category.items)
    .filter((dish) => dish.availableToday)

  async function onAdd() {
    if (!table || !dishId) return
    try {
      await addDish({
        tableId: table.tableId,
        restaurantId,
        menuItemId: dishId,
        quantity,
      }).unwrap()
      setDishId('')
      setQuantity(1)
    } catch {
      // addState.isError is shown below.
    }
  }

  return (
    <Dialog
      open={table !== null}
      onOpenChange={(open) => {
        if (open) return
        addState.reset()
        onClose()
      }}
    >
      <DialogContent className="max-h-[90svh] max-w-lg overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{table?.tableLabel}</DialogTitle>
          <DialogDescription>
            Məhsullar, kim nə vaxt əlavə edib və ödənişlər.
          </DialogDescription>
        </DialogHeader>

        <section aria-label="Məhsullar" className="space-y-2">
          <h3 className="text-sm font-semibold">Məhsullar</h3>
          {!order || order.items.length === 0 ? (
            <p className="text-sm text-muted-foreground">Hələ məhsul yoxdur.</p>
          ) : (
            <ul className="divide-y rounded-md border text-sm">
              {order.items.map((item) => (
                <li
                  key={item.id}
                  className="flex items-start justify-between gap-3 px-3 py-2"
                >
                  <div className="min-w-0">
                    <p className={item.paid ? 'line-through' : undefined}>
                      {item.quantity} x {item.name}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {lineAuthor(item)}
                      {item.addedAt && ` · ${formatDateTime(item.addedAt)}`}
                    </p>
                  </div>
                  <span className="shrink-0 tabular-nums">
                    {formatCurrency(item.total, order.currency)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section aria-label="Ödənişlər" className="space-y-2">
          <h3 className="text-sm font-semibold">Ödənişlər</h3>
          {payments.data?.length === 0 && (
            <p className="text-sm text-muted-foreground">
              Hələ ödəniş olmayıb.
            </p>
          )}
          {payments.isError && (
            <p className="text-sm text-destructive">
              Ödənişləri yükləmək alınmadı.
            </p>
          )}
          {payments.data && payments.data.length > 0 && table && (
            <ul className="divide-y rounded-md border text-sm">
              {payments.data.map((payment) => (
                <li
                  key={payment.id}
                  className="flex items-center justify-between gap-3 px-3 py-2"
                >
                  <div className="min-w-0 space-y-1">
                    <p className="tabular-nums">
                      {formatAmount(payment.totalAmount, table.currency)}
                      {payment.tipAmount > 0 &&
                        ` (bəxşiş ${formatAmount(payment.tipAmount, table.currency)})`}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {payment.splitType
                        ? SPLIT_LABELS[payment.splitType]
                        : '-'}
                      {' · '}
                      {formatDateTime(payment.createdAt)}
                    </p>
                  </div>
                  <TransactionStatusBadge status={payment.status} />
                </li>
              ))}
            </ul>
          )}
        </section>

        <section aria-label="Məhsul əlavə et" className="space-y-2">
          <h3 className="text-sm font-semibold">Məhsul əlavə et</h3>
          {/* Today's dishes as one-tap buttons: quicker than a dropdown on a
              waiter's tablet, and a dropdown inside this dialog would fight
              the dialog over keyboard focus. */}
          <ul
            className="grid max-h-48 grid-cols-2 gap-2 overflow-y-auto"
            aria-label="Bu günün menyusu"
          >
            {todaysDishes.map((dish) => {
              const picked = dish.id === dishId
              return (
                <li key={dish.id}>
                  <Button
                    type="button"
                    variant={picked ? 'secondary' : 'outline'}
                    aria-pressed={picked}
                    className={cn(
                      'h-auto w-full flex-col items-start gap-0 py-2 text-left',
                      picked && 'ring-2 ring-foreground',
                    )}
                    onClick={() => setDishId(picked ? '' : dish.id)}
                  >
                    <span className="w-full truncate">{dish.name}</span>
                    <span className="text-xs font-normal tabular-nums text-muted-foreground">
                      {formatAmount(dish.price, 'AZN')}
                    </span>
                  </Button>
                </li>
              )
            })}
          </ul>
          <div className="flex flex-wrap items-center justify-end gap-2">
            <div className="flex items-center gap-1">
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                disabled={quantity <= 1}
                aria-label="Azalt"
              >
                <Minus aria-hidden />
              </Button>
              <span
                className="w-6 text-center text-sm tabular-nums"
                aria-label="Say"
              >
                {quantity}
              </span>
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={() =>
                  setQuantity((q) => Math.min(MAX_QUANTITY, q + 1))
                }
                disabled={quantity >= MAX_QUANTITY}
                aria-label="Artır"
              >
                <Plus aria-hidden />
              </Button>
            </div>
            <Button
              type="button"
              onClick={() => void onAdd()}
              disabled={!dishId || addState.isLoading}
            >
              <Plus aria-hidden />
              {addState.isLoading ? 'Əlavə olunur...' : 'Əlavə et'}
            </Button>
          </div>
          {addState.isError && (
            <p className="text-sm text-destructive">
              Əlavə etmək alınmadı. Məhsul bu gün menyuda olmaya bilər.
            </p>
          )}
        </section>
        {canCancel && table && (
          <CancelOrderSection
            restaurantId={restaurantId}
            tableId={table.tableId}
            onCancelled={onClose}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}
