import { useCallback, useEffect, useMemo, useState } from 'react'
import type { CustomerDish } from './customer-menu-api'

// The customer's basket before they confirm (#140). It lives only on this
// phone: removing a dish here needs no server call, and nothing reaches the
// bill until the order is confirmed. Kept in localStorage per table so a
// reload does not lose it; storage may be unavailable (private mode), so every
// access is guarded and the basket then simply lasts for the page visit.

export const MAX_QUANTITY = 20

export interface BasketEntry {
  dish: CustomerDish
  quantity: number
}

const storageKey = (qrToken: string) => `masapay:basket:${qrToken}`

function load(qrToken: string): BasketEntry[] {
  try {
    const raw = window.localStorage.getItem(storageKey(qrToken))
    const parsed: unknown = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? (parsed as BasketEntry[]) : []
  } catch {
    return []
  }
}

function save(qrToken: string, entries: BasketEntry[]) {
  try {
    if (entries.length === 0) {
      window.localStorage.removeItem(storageKey(qrToken))
    } else {
      window.localStorage.setItem(storageKey(qrToken), JSON.stringify(entries))
    }
  } catch {
    // Storage is full or blocked: the basket still works for this visit.
  }
}

export function useBasket(qrToken: string) {
  const [entries, setEntries] = useState<BasketEntry[]>(() => load(qrToken))

  useEffect(() => save(qrToken, entries), [qrToken, entries])

  const change = useCallback((dish: CustomerDish, delta: number) => {
    setEntries((current) => {
      const found = current.find((entry) => entry.dish.id === dish.id)
      if (!found) {
        return delta > 0 ? [...current, { dish, quantity: 1 }] : current
      }
      const quantity = Math.min(MAX_QUANTITY, found.quantity + delta)
      return quantity <= 0
        ? current.filter((entry) => entry.dish.id !== dish.id)
        : current.map((entry) =>
            entry.dish.id === dish.id ? { ...entry, quantity } : entry,
          )
    })
  }, [])

  const remove = useCallback((dishId: string) => {
    setEntries((current) => current.filter((entry) => entry.dish.id !== dishId))
  }, [])

  const clear = useCallback(() => setEntries([]), [])

  /** Drops dishes that are no longer on today's menu and refreshes prices. */
  const syncWithMenu = useCallback((available: CustomerDish[]) => {
    const byId = new Map(available.map((dish) => [dish.id, dish]))
    setEntries((current) => {
      const next = current
        .filter((entry) => byId.has(entry.dish.id))
        .map((entry) => ({ ...entry, dish: byId.get(entry.dish.id)! }))
      const same =
        next.length === current.length &&
        next.every((entry, i) => entry.dish.price === current[i].dish.price)
      return same ? current : next
    })
  }, [])

  const total = useMemo(
    () =>
      entries.reduce(
        (sum, entry) => sum + entry.dish.price * entry.quantity,
        0,
      ),
    [entries],
  )
  const count = useMemo(
    () => entries.reduce((sum, entry) => sum + entry.quantity, 0),
    [entries],
  )
  const quantityOf = useCallback(
    (dishId: string) =>
      entries.find((entry) => entry.dish.id === dishId)?.quantity ?? 0,
    [entries],
  )

  return {
    entries,
    total,
    count,
    quantityOf,
    change,
    remove,
    clear,
    syncWithMenu,
  }
}
