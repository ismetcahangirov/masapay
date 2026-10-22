import { beforeAll, beforeEach, describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
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
        source: 'CUSTOMER',
        addedBy: null,
        addedAt: '2026-10-22T08:05:00Z',
      },
      {
        id: 'i2',
        name: 'Dolma',
        quantity: 1,
        unitPrice: 11.5,
        totalPrice: 11.5,
        paid: false,
        kitchenStatus: 'PREPARING',
        source: 'STAFF',
        addedBy: 'Aysel',
        addedAt: '2026-10-22T08:20:00Z',
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

  it('shows who added each line, the payments, and adds a menu dish (#144)', async () => {
    let sent: unknown = null
    mockApi({
      'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
      'GET /api/admin/restaurants/r1/floor/bills': bills,
      'GET /api/admin/tables/t1/order/payments': [
        {
          id: 'p1',
          amount: 8.5,
          tipAmount: 1,
          totalAmount: 9.5,
          refundedAmount: 0,
          splitType: 'ITEM',
          status: 'SUCCESS',
          createdAt: '2026-10-22T08:30:00Z',
        },
      ],
      'GET /api/admin/restaurants/r1/menu': [
        {
          id: 'c1',
          name: 'Əsas',
          sortOrder: 0,
          items: [
            {
              id: 'm1',
              categoryId: 'c1',
              name: 'Kabab',
              description: null,
              price: 12,
              components: [],
              active: true,
              offOn: null,
              availableFrom: null,
              availableUntil: null,
              availableToday: true,
              suggestionIds: [],
            },
            {
              id: 'm2',
              categoryId: 'c1',
              name: 'Dovğa',
              description: null,
              price: 3,
              components: [],
              active: true,
              offOn: '2026-10-22',
              availableFrom: null,
              availableUntil: null,
              availableToday: false,
              suggestionIds: [],
            },
          ],
        },
      ],
      'POST /api/admin/tables/t1/order/menu-items': async (
        request: Request,
      ) => {
        sent = await request.json()
        return bills[0]
      },
    })
    renderWithProviders(<OrdersPage />)

    await userEvent.click(
      await screen.findByRole('button', { name: 'Masa 1 ətraflı' }),
    )
    const dialog = screen.getByRole('dialog', { name: 'Masa 1' })
    const lines = within(dialog).getByRole('region', { name: 'Məhsullar' })
    expect(within(lines).getByText(/Müştəri \(QR\)/)).toBeInTheDocument()
    expect(within(lines).getByText(/Aysel/)).toBeInTheDocument()
    const payments = within(dialog).getByRole('region', { name: 'Ödənişlər' })
    expect(await within(payments).findByText(/bəxşiş/)).toBeInTheDocument()
    expect(within(payments).getByText(/Məhsula görə/)).toBeInTheDocument()

    const todays = within(dialog).getByRole('list', {
      name: 'Bu günün menyusu',
    })
    // A dish that is off today is not offered.
    expect(within(todays).queryByText('Dovğa')).not.toBeInTheDocument()
    await userEvent.click(within(todays).getByRole('button', { name: /Kabab/ }))
    expect(
      within(todays).getByRole('button', { name: /Kabab/ }),
    ).toHaveAttribute('aria-pressed', 'true')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Artır' }))
    await userEvent.click(
      within(dialog).getByRole('button', { name: 'Əlavə et' }),
    )

    await waitFor(() => expect(sent).toEqual({ menuItemId: 'm1', quantity: 2 }))
  })
})
