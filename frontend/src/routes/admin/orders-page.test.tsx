import { beforeAll, beforeEach, describe, expect, it } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { userLoaded } from '@/features/auth/auth-slice'
import { OrdersPage } from '@/routes/admin/orders-page'
import { mockApi, renderWithProviders } from '@/test/utils'

const order = (
  id: string,
  total: number,
  paid: number,
  items: object[],
): object => ({
  orderId: id,
  status: 'OPEN',
  totalAmount: total,
  paidAmount: paid,
  outstanding: total - paid,
  items,
  equalSplit: null,
  serviceCharge: null,
})

const bills = [
  {
    tableId: 't1',
    tableLabel: 'Masa 1',
    restaurantId: 'r1',
    restaurantName: 'Dolma',
    currency: 'AZN',
    serviceChargePercent: null,
    order: order('o1', 20, 8.5, [
      {
        id: 'i1',
        name: 'Sezar',
        quantity: 1,
        unitPrice: 8.5,
        totalPrice: 8.5,
        paid: true,
        kitchenStatus: 'READY',
      },
      {
        id: 'i2',
        name: 'Dolma',
        quantity: 1,
        unitPrice: 11.5,
        totalPrice: 11.5,
        paid: false,
        kitchenStatus: 'PREPARING',
      },
    ]),
  },
  {
    tableId: 't2',
    tableLabel: 'Masa 2',
    restaurantId: 'r1',
    restaurantName: 'Dolma',
    currency: 'AZN',
    serviceChargePercent: null,
    order: order('o2', 3, 0, [
      {
        id: 'i3',
        name: 'Çay',
        quantity: 2,
        unitPrice: 1.5,
        totalPrice: 3,
        paid: false,
        kitchenStatus: null,
      },
    ]),
  },
]

describe('OrdersPage (#143)', () => {
  beforeAll(() => {
    // Radix Select uses pointer capture and scrolling that jsdom lacks.
    Element.prototype.hasPointerCapture ??= () => false
    Element.prototype.releasePointerCapture ??= () => {}
    Element.prototype.scrollIntoView ??= () => {}
  })

  beforeEach(() => {
    store.dispatch(userLoaded({ id: 'u1', email: 'w@x.az', role: 'WAITER' }))
    mockApi({
      'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
      'GET /api/admin/restaurants/r1/floor/bills': bills,
    })
  })

  it('shows each table with what was ordered, the kitchen state and what is paid', async () => {
    renderWithProviders(<OrdersPage />)

    const first = await screen.findByRole('region', { name: 'Masa 1' })
    expect(within(first).getByText('1 x Sezar')).toHaveClass('line-through')
    expect(
      within(first).getByText('Ödənilib', { selector: 'div' }),
    ).toBeInTheDocument()
    expect(within(first).getByText('Hazırlanır')).toBeInTheDocument()
    expect(within(first).getByText('Hazırdır')).toBeInTheDocument()
    const totals = within(first).getByText('Qalıb').closest('dl') as HTMLElement
    expect(totals).toHaveTextContent(/Cəmi\s*20,00/)
    expect(totals).toHaveTextContent(/Qalıb\s*11,50/)

    const second = screen.getByRole('region', { name: 'Masa 2' })
    // Staff-typed lines never went to the kitchen: no kitchen badge.
    expect(within(second).queryByText(/Mətbəx|Hazır/)).not.toBeInTheDocument()
  })

  it('narrows the screen to one table', async () => {
    renderWithProviders(<OrdersPage />)
    await screen.findByRole('region', { name: 'Masa 2' })

    await userEvent.click(screen.getByRole('combobox', { name: 'Masa' }))
    await userEvent.click(screen.getByRole('option', { name: 'Masa 2' }))

    expect(
      screen.queryByRole('region', { name: 'Masa 1' }),
    ).not.toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Masa 2' })).toBeInTheDocument()
  })
})
