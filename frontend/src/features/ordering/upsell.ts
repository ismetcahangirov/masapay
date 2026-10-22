import type { CustomerDish } from './customer-menu-api'
import type { BasketEntry } from './use-basket'

// Upsell (#141): the extras to offer before confirming. They come from the
// dishes in the basket, in basket order and then the manager's order, each at
// most once. A dish already in the basket is not offered again, and one that
// is not on today's menu is not offered at all (it is missing from `menu`).
export function upsellFor(
  entries: BasketEntry[],
  menu: CustomerDish[],
): CustomerDish[] {
  const byId = new Map(menu.map((dish) => [dish.id, dish]))
  const inBasket = new Set(entries.map((entry) => entry.dish.id))
  const offered = new Map<string, CustomerDish>()
  for (const { dish } of entries) {
    const current = byId.get(dish.id) ?? dish
    // A basket saved before #141 has no suggestionIds on its dishes.
    for (const id of current.suggestionIds ?? []) {
      const extra = byId.get(id)
      if (extra && !inBasket.has(id) && !offered.has(id)) {
        offered.set(id, extra)
      }
    }
  }
  return [...offered.values()]
}
