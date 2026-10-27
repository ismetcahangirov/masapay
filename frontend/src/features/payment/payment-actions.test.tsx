import { beforeEach, describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { api } from '@/app/api'
import type { Order, OrderItem } from '@/features/order/types'
import { PaymentActions } from '@/features/payment/payment-actions'
import { paymentOffers } from '@/features/payment/payment-offer'
import type { CustomerDish } from '@/features/ordering/customer-menu-api'
import { mockApi, renderWithProviders } from '@/test/utils'

const QR = 'q1'
const BASE = `/api/public/tables/${QR}`

function line(id: string, menuItemId: string | null): OrderItem {
  return {
    id,
    name: id,
    quantity: 1,
    unitPrice: 900,
    total: 900,
    paid: false,
    kitchenStatus: null,
    source: 'CUSTOMER',
    addedBy: null,
    addedAt: null,
    menuItemId,
  }
}

function bill(items: OrderItem[], equalSplit: Order['equalSplit'] = null) {
  return {
    id: 'o1',
    status: 'open',
    currency: 'AZN',
    totalAmount: 900 * items.length,
    paidAmount: 0,
    equalSplit,
    serviceCharge: null,
    items,
  } as Order
}

const dish = (id: string, suggestionIds: string[] = []): CustomerDish => ({
  id,
  name: id,
  description: null,
  price: 150,
  components: [],
  suggestionIds,
  askBeforeConfirm: false,
  imageUrl: null,
})

describe('paymentOffers (#183)', () => {
  it('offers the extras of the dishes on the bill, but not what the table has', () => {
    const menu = [
      dish('dolma', ['ayran', 'tea']),
      dish('kebab', ['tea', 'ayran']),
      dish('ayran'),
      dish('tea'),
    ]
    expect(
      paymentOffers(bill([line('a', 'dolma'), line('b', 'kebab')]), menu).map(
        (d) => d.id,
      ),
    ).toEqual(['ayran', 'tea'])
    expect(
      paymentOffers(bill([line('a', 'dolma'), line('b', 'tea')]), menu).map(
        (d) => d.id,
      ),
    ).toEqual(['ayran'])
    // A typed line has no dish; an extra off today is not in the menu.
    expect(paymentOffers(bill([line('a', null)]), menu)).toEqual([])
    expect(
      paymentOffers(bill([line('a', 'dolma')]), [dish('dolma', ['ayran'])]),
    ).toEqual([])
  })
})

describe('PaymentActions offer before paying (#183)', () => {
  const menu = [
    {
      id: 'c1',
      name: 'Əsas',
      items: [
        {
          id: 'dolma',
          name: 'Dolma',
          description: null,
          price: 9,
          components: [],
          suggestionIds: ['ayran'],
        },
        {
          id: 'ayran',
          name: 'Ayran',
          description: null,
          price: 1.5,
          components: [],
          suggestionIds: [],
        },
      ],
    },
  ]

  beforeEach(() => {
    store.dispatch(api.util.resetApiState())
  })

  it('offers Ayran once, adds it to the bill and goes on to the payment', async () => {
    let sent: unknown = null
    mockApi({
      [`GET ${BASE}/menu`]: menu,
      [`GET ${BASE}/payment/full`]: { amount: 10.5, currency: 'AZN' },
      [`POST ${BASE}/order/items`]: async (request: Request) => {
        sent = await request.json()
        return {
          tableId: 't1',
          tableLabel: 'Masa 1',
          restaurantId: 'r1',
          restaurantName: 'Dolma',
          currency: 'AZN',
          serviceChargePercent: null,
          order: null,
        }
      },
    })
    renderWithProviders(
      <PaymentActions qrToken={QR} order={bill([line('a', 'dolma')])} />,
    )
    // The menu loads in the background.
    await waitFor(() =>
      expect(
        store.getState().api.queries[`customerMenu("${QR}")`]?.status,
      ).toBe('fulfilled'),
    )

    await userEvent.click(screen.getByRole('button', { name: 'Tam ödə' }))
    const offer = screen.getByRole('dialog', {
      name: 'Bu məhsulları da istəyirsiniz?',
    })
    await userEvent.click(
      within(offer).getByRole('button', { name: 'Ayran əlavə et' }),
    )
    expect(
      await within(offer).findByRole('button', { name: 'Ayran əlavə olundu' }),
    ).toBeDisabled()
    expect(sent).toEqual({ items: [{ menuItemId: 'ayran', quantity: 1 }] })

    await userEvent.click(
      within(offer).getByRole('button', { name: 'Ödənişə keç' }),
    )
    const pay = await screen.findByRole('dialog', { name: 'Tam ödə' })
    expect(
      await within(pay).findByRole('button', { name: /10,50.*ödə/ }),
    ).toBeEnabled()

    // Closing and paying again does not ask a second time.
    await userEvent.keyboard('{Escape}')
    await userEvent.click(screen.getByRole('button', { name: 'Tam ödə' }))
    expect(
      screen.queryByRole('dialog', { name: 'Bu məhsulları da istəyirsiniz?' }),
    ).toBeNull()
  })

  it('does not offer once an equal split has started', async () => {
    mockApi({
      [`GET ${BASE}/menu`]: menu,
      [`GET ${BASE}/payment/full`]: { amount: 9, currency: 'AZN' },
    })
    renderWithProviders(
      <PaymentActions
        qrToken={QR}
        order={bill([line('a', 'dolma')], { totalParts: 2, paidParts: 1 })}
      />,
    )
    await waitFor(() =>
      expect(
        store.getState().api.queries[`customerMenu("${QR}")`]?.status,
      ).toBe('fulfilled'),
    )

    await userEvent.click(screen.getByRole('button', { name: 'Tam ödə' }))

    expect(
      screen.queryByRole('dialog', { name: 'Bu məhsulları da istəyirsiniz?' }),
    ).toBeNull()
    expect(screen.getByRole('dialog', { name: 'Tam ödə' })).toBeInTheDocument()
  })
})
