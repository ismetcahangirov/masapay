import { Check } from 'lucide-react'
import { type OrderItem, orderItemTotal } from '@/features/order/types'
import { formatCurrency } from '@/features/order/format'
import { KitchenStatusBadge } from '@/features/order/kitchen-status-badge'
import { cn } from '@/lib/utils'

// One line of the bill. A paid line stays on the receipt but is crossed out
// and greyed, so everyone at the table sees what is settled and what is not.
// A dish from the menu also shows where it is in the kitchen (#178).
function OrderItemRow({
  item,
  currency,
}: {
  item: OrderItem
  currency: string
}) {
  return (
    <li
      className={cn(
        'flex items-start justify-between gap-3 py-3',
        item.paid && 'text-muted-foreground',
      )}
    >
      <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
        <span className="tabular-nums text-sm text-muted-foreground">
          {item.quantity}&times;
        </span>
        <span
          className={cn(
            'truncate text-sm font-medium',
            item.paid && 'line-through decoration-1',
          )}
        >
          {item.name}
        </span>
        {item.paid && (
          <span className="inline-flex shrink-0 items-center gap-0.5 text-xs text-success">
            <Check className="size-3" aria-hidden />
            Ödənilib
          </span>
        )}
        {item.kitchenStatus && !item.paid && (
          <KitchenStatusBadge status={item.kitchenStatus} />
        )}
      </div>
      <span
        className={cn(
          'shrink-0 tabular-nums text-sm font-medium',
          item.paid && 'line-through decoration-1',
        )}
      >
        {formatCurrency(orderItemTotal(item), currency)}
      </span>
    </li>
  )
}

export function OrderItemList({
  items,
  currency,
}: {
  items: OrderItem[]
  currency: string
}) {
  if (items.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-muted-foreground">
        Hələ sifariş əlavə olunmayıb.
      </p>
    )
  }

  return (
    <ul className="divide-y divide-border">
      {items.map((item) => (
        <OrderItemRow key={item.id} item={item} currency={currency} />
      ))}
    </ul>
  )
}
