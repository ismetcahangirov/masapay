// Domain types for a table's live bill (adisyon).
//
// These shapes are consumed by presentational components now (fed with mock
// data) and will be produced by the RTK Query order API in sub-issue 4.2.

export type OrderStatus =
  /** Table is seated and the bill is still growing. */
  | 'open'
  /** Kitchen has closed the bill; awaiting the customer's payment. */
  | 'awaiting_payment'
  /** Some guests have paid their share; a balance remains. */
  | 'partially_paid'
  /** Fully settled. */
  | 'paid'

export interface OrderItem {
  id: string
  name: string
  quantity: number
  /** Unit price in minor currency units (qəpik). */
  unitPrice: number
}

export interface Order {
  id: string
  tableId: string
  restaurantSlug: string
  status: OrderStatus
  items: OrderItem[]
  /** ISO-4217 currency code, e.g. "AZN". */
  currency: string
  /** Amount already paid, in minor currency units. */
  paidAmount: number
}

/** Line total in minor currency units. */
export function orderItemTotal(item: OrderItem): number {
  return item.quantity * item.unitPrice
}

/** Bill total in minor currency units. */
export function orderTotal(order: Order): number {
  return order.items.reduce((sum, item) => sum + orderItemTotal(item), 0)
}

/** Outstanding balance in minor currency units, never negative. */
export function orderBalance(order: Order): number {
  return Math.max(0, orderTotal(order) - order.paidAmount)
}
