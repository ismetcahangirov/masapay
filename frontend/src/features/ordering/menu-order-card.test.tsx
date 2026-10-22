import { beforeEach, describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MenuOrderCard } from '@/features/ordering/menu-order-card'
import { mockApi, renderWithProviders, requestsOf } from '@/test/utils'

const QR = 'q1'
const BASE = `/api/public/tables/${QR}`

const menu = [
  {
    id: 'c1',
    name: 'Əsas',
    items: [
      { id: 'd1', name: 'Dolma', description: null, price: 9, components: [] },
      {
        id: 'd2',
        name: 'Ayran',
        description: null,
        price: 1.5,
        components: [],
      },
    ],
  },
  {
    id: 'c2',
    name: 'Biznes lanç',
    items: [
      {
        id: 'k1',
        name: 'Lanç 1',
        description: null,
        price: 11,
        components: [{ name: 'Mərci', quantity: 1 }],
      },
    ],
  },
]

const bill = {
  tableId: 't1',
  tableLabel: 'Masa 1',
  restaurantId: 'r1',
  restaurantName: 'Dolma',
  currency: 'AZN',
  serviceChargePercent: null,
  order: null,
}

describe('MenuOrderCard (#140)', () => {
  beforeEach(() => window.localStorage.clear())

  it('builds a basket, drops a dish before confirming and confirms after "Əminsiniz?"', async () => {
    let sent: unknown = null
    const fetchMock = mockApi({
      [`GET ${BASE}/menu`]: menu,
      [`POST ${BASE}/order/items`]: async (request: Request) => {
        sent = await request.json()
        return bill
      },
    })
    renderWithProviders(<MenuOrderCard qrToken={QR} currency="AZN" />)

    expect(await screen.findByText('Kombo')).toBeInTheDocument()
    await userEvent.click(
      screen.getByRole('button', { name: 'Dolma əlavə et' }),
    )
    await userEvent.click(screen.getByRole('button', { name: 'Dolma artır' }))
    await userEvent.click(
      screen.getByRole('button', { name: 'Ayran əlavə et' }),
    )
    // Not wanted after all: removed before confirming, no server call needed.
    await userEvent.click(
      screen.getByRole('button', { name: 'Ayran səbətdən çıxar' }),
    )

    const basket = screen.getByRole('list', { name: 'Səbət' })
    expect(within(basket).getByText('2 x Dolma')).toBeInTheDocument()
    expect(within(basket).queryByText(/Ayran/)).not.toBeInTheDocument()

    await userEvent.click(
      screen.getByRole('button', { name: /Sifarişi təsdiqlə/ }),
    )
    const dialog = screen.getByRole('dialog', { name: 'Əminsiniz?' })
    expect(
      within(dialog).getByText('Təsdiqdən sonra sifarişi silmək olmur.'),
    ).toBeInTheDocument()
    expect(requestsOf(fetchMock)).not.toContain(`POST ${BASE}/order/items`)
    await userEvent.click(
      within(dialog).getByRole('button', { name: 'Bəli, təsdiqlə' }),
    )

    expect(
      await screen.findByText('Sifarişiniz masanın hesabına əlavə olundu.'),
    ).toBeInTheDocument()
    expect(sent).toEqual({ items: [{ menuItemId: 'd1', quantity: 2 }] })
    // The confirmed round is on the bill now: no basket, nothing to remove here.
    expect(
      screen.queryByRole('list', { name: 'Səbət' }),
    ).not.toBeInTheDocument()
    expect(window.localStorage.getItem(`masapay:basket:${QR}`)).toBeNull()
  })

  it('opens the menu again with "Əlavə sifariş" after an order', async () => {
    mockApi({
      [`GET ${BASE}/menu`]: menu,
      [`POST ${BASE}/order/items`]: bill,
    })
    renderWithProviders(<MenuOrderCard qrToken={QR} currency="AZN" />)

    await userEvent.click(
      await screen.findByRole('button', { name: 'Ayran əlavə et' }),
    )
    await userEvent.click(
      screen.getByRole('button', { name: /Sifarişi təsdiqlə/ }),
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Bəli, təsdiqlə' }),
    )
    await screen.findByText('Sifarişiniz masanın hesabına əlavə olundu.')
    expect(
      screen.queryByRole('button', { name: 'Ayran əlavə et' }),
    ).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /Əlavə sifariş/ }))

    expect(
      screen.getByRole('button', { name: 'Ayran əlavə et' }),
    ).toBeInTheDocument()
  })

  it('keeps the basket over a reload of the page', async () => {
    mockApi({ [`GET ${BASE}/menu`]: menu })
    const first = renderWithProviders(
      <MenuOrderCard qrToken={QR} currency="AZN" />,
    )
    await userEvent.click(
      await screen.findByRole('button', { name: 'Dolma əlavə et' }),
    )
    first.unmount()

    renderWithProviders(<MenuOrderCard qrToken={QR} currency="AZN" />)

    const basket = await screen.findByRole('list', { name: 'Səbət' })
    expect(within(basket).getByText('1 x Dolma')).toBeInTheDocument()
  })

  it('reloads the menu and explains when a dish went off before confirming', async () => {
    const fetchMock = mockApi({
      [`GET ${BASE}/menu`]: menu,
      [`POST ${BASE}/order/items`]: new Response(
        JSON.stringify({ status: 409 }),
        { status: 409 },
      ),
    })
    renderWithProviders(<MenuOrderCard qrToken={QR} currency="AZN" />)

    await userEvent.click(
      await screen.findByRole('button', { name: 'Dolma əlavə et' }),
    )
    await userEvent.click(
      screen.getByRole('button', { name: /Sifarişi təsdiqlə/ }),
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Bəli, təsdiqlə' }),
    )

    expect(
      await screen.findByText(/Bəzi məhsullar artıq mövcud deyil/),
    ).toBeInTheDocument()
    // The error stays in the dialog; behind it the menu is loaded again.
    await waitFor(() =>
      expect(
        requestsOf(fetchMock).filter((r) => r === `GET ${BASE}/menu`),
      ).toHaveLength(2),
    )
  })
})
