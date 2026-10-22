import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BillCard } from '@/features/order/bill-card'
import type { TableOrder } from '@/features/order/types'

function table(overrides: Partial<NonNullable<TableOrder['order']>> = {}) {
  const order: NonNullable<TableOrder['order']> = {
    id: 'o1',
    status: 'partially_paid',
    currency: 'AZN',
    totalAmount: 2970,
    paidAmount: 990,
    equalSplit: null,
    serviceCharge: { amount: 270, guests: null },
    items: [
      {
        id: 'a',
        name: 'Lülə kabab',
        quantity: 1,
        unitPrice: 1800,
        total: 1800,
        paid: false,
        kitchenStatus: null,
        source: 'STAFF',
        addedBy: null,
        addedAt: null,
      },
      {
        id: 'b',
        name: 'Mərci şorbası',
        quantity: 1,
        unitPrice: 900,
        total: 900,
        paid: true,
        kitchenStatus: null,
        source: 'STAFF',
        addedBy: null,
        addedAt: null,
      },
    ],
    ...overrides,
  }
  const value: TableOrder = {
    tableId: 't1',
    tableLabel: 'Masa 3',
    restaurantName: 'Dolma Restoran',
    currency: 'AZN',
    serviceChargePercent: 10,
    order,
  }
  return value
}

describe('BillCard (receipt)', () => {
  it('crosses out paid lines and keeps unpaid ones normal', () => {
    const data = table()
    render(<BillCard table={data} order={data.order!} />)

    expect(screen.getByText('Mərci şorbası')).toHaveClass('line-through')
    expect(screen.getByText('Lülə kabab')).not.toHaveClass('line-through')
  })

  it('shows the service charge as its own line with the percentage', () => {
    const data = table()
    render(<BillCard table={data} order={data.order!} />)

    expect(screen.getByText('Xidmət haqqı (10%)')).toBeInTheDocument()
    expect(screen.getByText('Məhsullar')).toBeInTheDocument()
  })

  it('shows a paid banner once the bill is fully paid', () => {
    const data = table({ status: 'paid', paidAmount: 2970 })
    render(<BillCard table={data} order={data.order!} />)

    expect(screen.getByText(/Hesab tam ödənilib/)).toBeInTheDocument()
  })
})
