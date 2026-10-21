import { describe, expect, it } from 'vitest'
import {
  type TableOrderDto,
  toMinor,
  toTableOrder,
} from '@/features/order/order-api'

const base: TableOrderDto = {
  tableId: 't1',
  tableLabel: 'Masa 3',
  restaurantId: 'r1',
  restaurantName: 'Dolma Restoran',
  currency: 'AZN',
  serviceChargePercent: null,
  order: {
    orderId: 'o1',
    status: 'OPEN',
    totalAmount: 27,
    paidAmount: 18,
    outstanding: 9,
    items: [
      {
        id: 'a',
        name: 'Lülə kabab',
        quantity: 1,
        unitPrice: 18,
        totalPrice: 18,
        paid: false,
      },
      {
        id: 'b',
        name: 'Mərci şorbası',
        quantity: 1,
        unitPrice: 9,
        totalPrice: 9,
        paid: true,
      },
    ],
    equalSplit: { totalParts: 2, paidParts: 1 },
    serviceCharge: null,
  },
}

describe('toTableOrder', () => {
  it('turns decimal money into qəpik without float noise', () => {
    expect(toMinor(0.1 + 0.2)).toBe(30)
    expect(toMinor(21.84)).toBe(2184)
  })

  it('takes paid and remaining from the backend, even with mixed split modes', () => {
    const order = toTableOrder(base).order!

    expect(order.totalAmount).toBe(2700)
    expect(order.paidAmount).toBe(1800)
    expect(order.status).toBe('partially_paid')
    expect(order.items.map((item) => item.paid)).toEqual([false, true])
  })

  it('shows every line as paid on a fully paid bill', () => {
    const order = toTableOrder({
      ...base,
      order: { ...base.order!, status: 'PAID', paidAmount: 27, outstanding: 0 },
    }).order!

    expect(order.status).toBe('paid')
    expect(order.items.every((item) => item.paid)).toBe(true)
  })

  it('keeps a table without an open bill as an empty state', () => {
    expect(toTableOrder({ ...base, order: null }).order).toBeNull()
  })
})
