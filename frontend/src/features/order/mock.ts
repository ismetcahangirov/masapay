import type { Order } from '@/features/order/types'

// Placeholder bill used while the order API (sub-issue 4.2) is not yet wired.
// Replaced by the RTK Query feed; kept isolated so the swap is a one-line
// change in TablePage.
export function mockOrder(restaurantSlug: string, tableId: string): Order {
  return {
    id: 'mock-order',
    tableId,
    restaurantSlug,
    status: 'awaiting_payment',
    currency: 'AZN',
    paidAmount: 0,
    items: [
      { id: '1', name: 'Kabab (qarışıq)', quantity: 2, unitPrice: 1450 },
      { id: '2', name: 'Çoban salatı', quantity: 1, unitPrice: 650 },
      { id: '3', name: 'Ayran', quantity: 3, unitPrice: 180 },
      { id: '4', name: 'Çay (dəst)', quantity: 1, unitPrice: 500 },
    ],
  }
}
