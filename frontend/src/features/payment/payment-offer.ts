import type { Order } from '@/features/order/types'
import type { CustomerDish } from '@/features/ordering/customer-menu-api'

/**
 * What to offer when the guest goes to pay (#183): the extras the manager set
 * for the dishes on the bill (#141), in bill order and then the manager's
 * order, each once. A dish the table already has is not offered, and one that
 * is not on today's menu is not offered at all (it is missing from `menu`).
 */
export function paymentOffers(
  order: Order,
  menu: CustomerDish[],
): CustomerDish[] {
  const byId = new Map(menu.map((dish) => [dish.id, dish]))
  const onBill = new Set(
    order.items.map((item) => item.menuItemId).filter((id) => !!id),
  )
  const offered = new Map<string, CustomerDish>()
  for (const item of order.items) {
    const dish = item.menuItemId ? byId.get(item.menuItemId) : undefined
    for (const id of dish?.suggestionIds ?? []) {
      const extra = byId.get(id)
      if (extra && !onBill.has(id) && !offered.has(id)) {
        offered.set(id, extra)
      }
    }
  }
  return [...offered.values()]
}
