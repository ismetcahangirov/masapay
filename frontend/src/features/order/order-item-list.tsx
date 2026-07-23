import { type OrderItem, orderItemTotal } from '@/features/order/types'
import { formatCurrency } from '@/features/order/format'

function OrderItemRow({
  item,
  currency,
}: {
  item: OrderItem
  currency: string
}) {
  return (
    <li className="flex items-start justify-between gap-3 py-3">
      <div className="flex min-w-0 items-baseline gap-2">
        <span className="tabular-nums text-sm text-muted-foreground">
          {item.quantity}&times;
        </span>
        <span className="truncate text-sm font-medium">{item.name}</span>
      </div>
      <span className="shrink-0 tabular-nums text-sm font-medium">
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
