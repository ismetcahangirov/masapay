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
  /** A manager cancelled the confirmed order (#186). */
  | 'cancelled'

/** Kitchen progress of an ordered dish (#142). */
export type KitchenStatus = 'NEW' | 'PREPARING' | 'READY'

export type LineSource = 'STAFF' | 'CUSTOMER' | 'POS'

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
  /** Where the dish is in the kitchen (#142); null when it never goes there. */
  kitchenStatus: KitchenStatus | null
  /** Who put it on the bill (#144): staff (by name), a customer from the menu, or the POS. */
  source: LineSource
  addedBy: string | null
  /** When it was added (ISO time). */
  addedAt: string | null
  /** The menu dish it came from (#183); null for a typed or POS line. */
  menuItemId?: string | null
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
  /** Who serves the table and gets its tips (#182); staff screens only. */
  waiter?: { id: string; name: string } | null
}

/** The table a QR code belongs to, with its open bill (null when there is none). */
export interface TableOrder {
  tableId: string
  tableLabel: string
  restaurantName: string
  currency: string
  /** Restaurant service charge in percent (e.g. 10), null when it charges none. */
  serviceChargePercent: number | null
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
