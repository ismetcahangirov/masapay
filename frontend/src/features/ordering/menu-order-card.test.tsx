import { beforeEach, describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MenuOrderCard } from '@/features/ordering/menu-order-card'
import { mockApi, renderWithProviders, requestsOf } from '@/test/utils'

const QR = 'q1'

/** The menu opens from the card's "Menyu" button (#180). */
async function openMenu() {
  await userEvent.click(await screen.findByRole('button', { name: 'Menyu' }))
}
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
    await openMenu()

    await userEvent.click(
      await screen.findByRole('button', { name: 'Dolma əlavə et' }),
    )
    await userEvent.click(screen.getByRole('button', { name: 'Dolma artır' }))
    await userEvent.click(
      screen.getByRole('button', { name: 'Ayran əlavə et' }),
    )
    // Not wanted after all: removed before confirming, no server call needed.
    await userEvent.click(
      screen.getByRole('button', { name: 'Ayran səbətdən çıxar' }),
    )

    const picked = screen.getByRole('complementary', { name: 'Seçilənlər' })
    expect(within(picked).getByText('Dolma')).toBeInTheDocument()
    expect(within(picked).getByText(/^2 x/)).toBeInTheDocument()
    expect(within(picked).queryByText(/Ayran/)).not.toBeInTheDocument()

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
    await openMenu()

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
    await openMenu()
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
    await openMenu()

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

  describe('upsell before confirming (#141)', () => {
    const withExtras = [
      {
        id: 'c1',
        name: 'Əsas',
        items: [
          {
            id: 'd1',
            name: 'Dolma',
            description: null,
            price: 9,
            components: [],
            suggestionIds: ['d2', 's1'],
          },
          {
            id: 'd2',
            name: 'Ayran',
            description: null,
            price: 1.5,
            components: [],
            suggestionIds: [],
          },
          {
            id: 's1',
            name: 'Qatıq sousu',
            description: null,
            price: 0.5,
            components: [],
            suggestionIds: [],
          },
        ],
      },
    ]

    it('adds a suggested extra in one click and then asks "Əminsiniz?"', async () => {
      let sent: unknown = null
      mockApi({
        [`GET ${BASE}/menu`]: withExtras,
        [`POST ${BASE}/order/items`]: async (request: Request) => {
          sent = await request.json()
          return bill
        },
      })
      renderWithProviders(<MenuOrderCard qrToken={QR} currency="AZN" />)
      await openMenu()

      await userEvent.click(
        await screen.findByRole('button', { name: 'Dolma əlavə et' }),
      )
      await userEvent.click(
        screen.getByRole('button', { name: /Sifarişi təsdiqlə/ }),
      )

      const upsell = screen.getByRole('dialog', { name: 'Bunu da istərdiniz?' })
      expect(
        screen.queryByRole('dialog', { name: 'Əminsiniz?' }),
      ).not.toBeInTheDocument()
      await userEvent.click(
        within(upsell).getByRole('button', { name: 'Ayran əlavə et' }),
      )
      expect(
        within(upsell).getByRole('button', { name: 'Ayran əlavə olundu' }),
      ).toBeDisabled()
      await userEvent.click(
        within(upsell).getByRole('button', { name: 'Davam et' }),
      )

      const confirm = screen.getByRole('dialog', { name: 'Əminsiniz?' })
      expect(within(confirm).getByText('1 x Ayran')).toBeInTheDocument()
      await userEvent.click(
        within(confirm).getByRole('button', { name: 'Bəli, təsdiqlə' }),
      )

      await screen.findByText('Sifarişiniz masanın hesabına əlavə olundu.')
      expect(sent).toEqual({
        items: [
          { menuItemId: 'd1', quantity: 1 },
          { menuItemId: 'd2', quantity: 1 },
        ],
      })
    })

    it('goes on without extras after "Xeyr, təşəkkürlər"', async () => {
      mockApi({ [`GET ${BASE}/menu`]: withExtras })
      renderWithProviders(<MenuOrderCard qrToken={QR} currency="AZN" />)
      await openMenu()

      await userEvent.click(
        await screen.findByRole('button', { name: 'Dolma əlavə et' }),
      )
      await userEvent.click(
        screen.getByRole('button', { name: /Sifarişi təsdiqlə/ }),
      )
      await userEvent.click(
        screen.getByRole('button', { name: 'Xeyr, təşəkkürlər' }),
      )

      const confirm = screen.getByRole('dialog', { name: 'Əminsiniz?' })
      expect(within(confirm).getByText('1 x Dolma')).toBeInTheDocument()
      expect(within(confirm).queryByText(/Ayran/)).not.toBeInTheDocument()
    })

    it('skips the offer when the basket has nothing to suggest', async () => {
      mockApi({ [`GET ${BASE}/menu`]: withExtras })
      renderWithProviders(<MenuOrderCard qrToken={QR} currency="AZN" />)
      await openMenu()

      await userEvent.click(
        await screen.findByRole('button', { name: 'Ayran əlavə et' }),
      )
      await userEvent.click(
        screen.getByRole('button', { name: /Sifarişi təsdiqlə/ }),
      )

      expect(
        screen.getByRole('dialog', { name: 'Əminsiniz?' }),
      ).toBeInTheDocument()
      expect(
        screen.queryByRole('dialog', { name: 'Bunu da istərdiniz?' }),
      ).not.toBeInTheDocument()
    })
  })

  describe('extras asked before confirming (#185)', () => {
    const withAsked = [
      {
        id: 'c1',
        name: 'Əsas',
        items: [
          {
            id: 'd1',
            name: 'Dolma',
            description: null,
            price: 9,
            components: [],
            suggestionIds: [],
            askBeforeConfirm: false,
          },
        ],
      },
      {
        id: 'c2',
        name: 'Souslar və içkilər',
        items: [
          {
            id: 'x1',
            name: 'Ketçup',
            description: null,
            price: 0,
            components: [],
            suggestionIds: [],
            askBeforeConfirm: true,
          },
          {
            id: 'x2',
            name: 'Su',
            description: null,
            price: 0.8,
            components: [],
            suggestionIds: [],
            askBeforeConfirm: true,
          },
        ],
      },
    ]

    it('asks "Ketçup və ya Su istəyirsiniz?" and adds what was chosen', async () => {
      let sent: unknown = null
      mockApi({
        [`GET ${BASE}/menu`]: withAsked,
        [`POST ${BASE}/order/items`]: async (request: Request) => {
          sent = await request.json()
          return bill
        },
      })
      renderWithProviders(<MenuOrderCard qrToken={QR} currency="AZN" />)
      await openMenu()

      await userEvent.click(
        await screen.findByRole('button', { name: 'Dolma əlavə et' }),
      )
      await userEvent.click(
        screen.getByRole('button', { name: /Sifarişi təsdiqlə/ }),
      )

      const ask = screen.getByRole('dialog', {
        name: 'Ketçup və ya Su istəyirsiniz?',
      })
      expect(within(ask).getByText('Pulsuz')).toBeInTheDocument()
      await userEvent.click(
        within(ask).getByRole('button', { name: 'Ketçup artır' }),
      )
      await userEvent.click(
        within(ask).getByRole('button', { name: 'Ketçup artır' }),
      )
      expect(within(ask).getByLabelText('Ketçup sayı')).toHaveTextContent('2')
      await userEvent.click(
        within(ask).getByRole('button', { name: 'Davam et' }),
      )

      const confirm = screen.getByRole('dialog', { name: 'Əminsiniz?' })
      expect(within(confirm).getByText('2 x Ketçup')).toBeInTheDocument()
      await userEvent.click(
        within(confirm).getByRole('button', { name: 'Bəli, təsdiqlə' }),
      )
      await screen.findByText('Sifarişiniz masanın hesabına əlavə olundu.')
      expect(sent).toEqual({
        items: [
          { menuItemId: 'd1', quantity: 1 },
          { menuItemId: 'x1', quantity: 2 },
        ],
      })
    })

    it('goes straight to "Əminsiniz?" after "Xeyr, təşəkkürlər"', async () => {
      mockApi({ [`GET ${BASE}/menu`]: withAsked })
      renderWithProviders(<MenuOrderCard qrToken={QR} currency="AZN" />)
      await openMenu()

      await userEvent.click(
        await screen.findByRole('button', { name: 'Dolma əlavə et' }),
      )
      await userEvent.click(
        screen.getByRole('button', { name: /Sifarişi təsdiqlə/ }),
      )
      await userEvent.click(
        screen.getByRole('button', { name: 'Xeyr, təşəkkürlər' }),
      )

      const confirm = screen.getByRole('dialog', { name: 'Əminsiniz?' })
      expect(within(confirm).getByText('1 x Dolma')).toBeInTheDocument()
      expect(within(confirm).queryByText(/Ketçup|Su/)).not.toBeInTheDocument()
    })
  })

  it('shows categories on the left, their dishes in the middle and the picks on the right (#180)', async () => {
    mockApi({ [`GET ${BASE}/menu`]: menu })
    renderWithProviders(<MenuOrderCard qrToken={QR} currency="AZN" />)
    await openMenu()

    const categories = screen.getByRole('navigation', { name: 'Kateqoriyalar' })
    const first = within(categories).getByRole('button', { name: 'Əsas' })
    expect(first).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('region', { name: 'Əsas' })).toHaveTextContent(
      'Dolma',
    )
    expect(screen.queryByText('Lanç 1')).not.toBeInTheDocument()

    await userEvent.click(
      within(categories).getByRole('button', { name: 'Biznes lanç' }),
    )
    const lunches = screen.getByRole('region', { name: 'Biznes lanç' })
    expect(within(lunches).getByText('Kombo')).toBeInTheDocument()
    await userEvent.click(
      within(lunches).getByRole('button', { name: 'Lanç 1 əlavə et' }),
    )

    const picked = screen.getByRole('complementary', { name: 'Seçilənlər' })
    expect(within(picked).getByText('Lanç 1')).toBeInTheDocument()
    await userEvent.click(
      within(picked).getByRole('button', { name: 'Seçilən Lanç 1 artır' }),
    )
    expect(within(picked).getByText(/^2 x/)).toBeInTheDocument()
    expect(
      within(picked).getByRole('button', { name: /Sifarişi təsdiqlə/ }),
    ).toHaveTextContent('22')
  })
})
