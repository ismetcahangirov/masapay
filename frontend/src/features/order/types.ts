// Domain types for a table's live bill (adisyon), as the UI uses them. The
// backend's JSON is mapped into these shapes in `order-api.ts`.

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
  /** Line total in minor currency units. */
  total: number
  /** Already settled by an item payment. */
  paid: boolean
}

export interface Order {
  id: string
  status: OrderStatus
  items: OrderItem[]
  /** Bill total in minor units, including any service charge. */
  totalAmount: number
  /** Amount already paid, in minor currency units. */
  paidAmount: number
  /** ISO-4217 currency code, e.g. "AZN". */
  currency: string
  /** Progress of an equal split, once someone has started one (#26). */
  equalSplit: { totalParts: number; paidParts: number } | null
  /** Charge not tied to an item, shared by the guests (#25); null when there is none. */
  serviceCharge: { amount: number; guests: number | null } | null
}

/** The table a QR code belongs to, with its open bill (null when there is none). */
export interface TableOrder {
  tableId: string
  tableLabel: string
  restaurantName: string
  currency: string
  order: Order | null
}

/** Line total in minor currency units. */
export function orderItemTotal(item: OrderItem): number {
  return item.total
}

/** Bill total in minor currency units. */
export function orderTotal(order: Order): number {
  return order.totalAmount
}

/** Outstanding balance in minor currency units, never negative. */
export function orderBalance(order: Order): number {
  return Math.max(0, order.totalAmount - order.paidAmount)
}
