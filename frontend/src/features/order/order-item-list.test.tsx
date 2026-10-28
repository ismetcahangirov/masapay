import { describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { OrderItemList } from '@/features/order/order-item-list'
import type { OrderItem } from '@/features/order/types'
import i18n from '@/i18n'

function item(overrides: Partial<OrderItem>): OrderItem {
  return {
    id: 'a',
    name: 'Sezar',
    quantity: 1,
    unitPrice: 850,
    total: 850,
    paid: false,
    kitchenStatus: null,
    source: 'CUSTOMER',
    addedBy: null,
    addedAt: null,
    ...overrides,
  }
}

describe('OrderItemList kitchen status (#178)', () => {
  it('shows the customer where each dish is in the kitchen', () => {
    render(
      <OrderItemList
        currency="AZN"
        items={[
          item({ id: 'a', name: 'Sezar', kitchenStatus: 'READY' }),
          item({ id: 'b', name: 'Dolma', kitchenStatus: 'PREPARING' }),
          item({ id: 'c', name: 'Çay', kitchenStatus: 'NEW' }),
          item({ id: 'd', name: 'Su', kitchenStatus: null }),
          item({ id: 'e', name: 'Kabab', kitchenStatus: 'READY', paid: true }),
        ]}
      />,
    )

    const row = (name: string) => screen.getByText(name).closest('li')!
    expect(within(row('Sezar')).getByText('Hazırdır')).toBeInTheDocument()
    expect(within(row('Dolma')).getByText('Hazırlanır')).toBeInTheDocument()
    expect(within(row('Çay')).getByText('Qəbul olundu')).toBeInTheDocument()
    // A line added by hand never goes to the kitchen; a paid one is done.
    expect(within(row('Su')).queryByText(/Hazır|Qəbul/)).toBeNull()
    expect(within(row('Kabab')).queryByText('Hazırdır')).toBeNull()
  })
})

describe('OrderItemList in English (#215)', () => {
  it('shows the kitchen status, paid mark and amount in English', async () => {
    await i18n.changeLanguage('en')
    render(
      <OrderItemList
        currency="AZN"
        items={[
          item({ id: 'a', name: 'Sezar', kitchenStatus: 'PREPARING' }),
          item({ id: 'b', name: 'Kabab', paid: true }),
        ]}
      />,
    )

    const row = (name: string) => screen.getByText(name).closest('li')!
    expect(within(row('Sezar')).getByText('Preparing')).toBeInTheDocument()
    expect(within(row('Kabab')).getByText('Paid')).toBeInTheDocument()
    expect(screen.queryByText('Ödənilib')).toBeNull()
  })
})
