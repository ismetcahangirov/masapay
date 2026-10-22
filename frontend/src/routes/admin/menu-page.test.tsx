import { beforeEach, describe, expect, it } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { type UserRole, userLoaded } from '@/features/auth/auth-slice'
import { parsePrice } from '@/features/admin/menu/menu-price'
import { MenuPage } from '@/routes/admin/menu-page'
import { mockApi, renderWithProviders } from '@/test/utils'

const MENU_URL = '/api/admin/restaurants/r1/menu'

const menu = [
  {
    id: 'c1',
    name: 'Salatlar',
    sortOrder: 0,
    items: [
      {
        id: 'd1',
        categoryId: 'c1',
        name: 'Sezar',
        description: 'Toyuq, kahı',
        price: 8.5,
        components: [],
      },
    ],
  },
  { id: 'c2', name: 'İçkilər', sortOrder: 1, items: [] },
  {
    id: 'c3',
    name: 'Biznes lanç',
    sortOrder: 2,
    items: [
      {
        id: 'k1',
        categoryId: 'c3',
        name: 'Lanç 1',
        description: null,
        price: 12,
        components: [{ itemId: 'd1', name: 'Sezar', quantity: 2 }],
      },
    ],
  },
]

function signInAs(role: UserRole) {
  store.dispatch(userLoaded({ id: 'u1', email: 'u@x.az', role }))
}

function backend(extra: Record<string, unknown> = {}) {
  return mockApi({
    'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
    [`GET ${MENU_URL}`]: menu,
    ...extra,
  })
}

describe('parsePrice', () => {
  it('reads manat with a dot or a comma and rejects anything else', () => {
    expect(parsePrice('8.50')).toBe(8.5)
    expect(parsePrice(' 8,5 ')).toBe(8.5)
    expect(parsePrice('12')).toBe(12)
    expect(parsePrice('8.505')).toBeNull()
    expect(parsePrice('-1')).toBeNull()
    expect(parsePrice('abc')).toBeNull()
  })
})

describe('MenuPage (#137)', () => {
  beforeEach(() => signInAs('RESTO_MANAGER'))

  it('shows categories in order with their dishes and prices', async () => {
    backend()
    renderWithProviders(<MenuPage />)

    expect(
      await screen.findByRole('heading', { name: 'Salatlar' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'İçkilər' })).toBeInTheDocument()
    expect(screen.getByText('Sezar')).toBeInTheDocument()
    expect(screen.getByText('Toyuq, kahı')).toBeInTheDocument()
    expect(screen.getByText(/8,50/)).toBeInTheDocument()
    expect(
      screen.getByText('Bu kateqoriyada məhsul yoxdur.'),
    ).toBeInTheDocument()
  })

  it('adds a dish to a category, sending the comma price as a number', async () => {
    let sent: unknown = null
    backend({
      [`POST ${MENU_URL}/items`]: async (request: Request) => {
        sent = await request.json()
        return { id: 'd2', ...(sent as object) }
      },
    })
    renderWithProviders(<MenuPage />)

    const drinks = await screen.findByRole('region', { name: 'İçkilər' })
    await userEvent.click(
      within(drinks).getByRole('button', { name: /Məhsul/ }),
    )
    await userEvent.type(screen.getByLabelText('Ad'), 'Ayran')
    await userEvent.type(screen.getByLabelText('Qiymət (AZN)'), '2,5')
    await userEvent.click(screen.getByRole('button', { name: 'Yadda saxla' }))

    await screen.findByRole('heading', { name: 'İçkilər' })
    expect(sent).toEqual({
      categoryId: 'c2',
      name: 'Ayran',
      description: null,
      price: 2.5,
      components: [],
    })
  })

  it('shows a combo with its badge and contents (#138)', async () => {
    backend()
    renderWithProviders(<MenuPage />)

    const lunch = await screen.findByRole('region', { name: 'Biznes lanç' })
    const row = within(lunch).getByRole('listitem')
    expect(within(row).getByText('Kombo')).toBeInTheDocument()
    expect(within(row).getByText('2 x Sezar')).toBeInTheDocument()
  })

  it('creates a combo from picked dishes with its own price (#138)', async () => {
    let sent: unknown = null
    backend({
      [`POST ${MENU_URL}/items`]: async (request: Request) => {
        sent = await request.json()
        return { id: 'k2', ...(sent as object) }
      },
    })
    renderWithProviders(<MenuPage />)

    const lunch = await screen.findByRole('region', { name: 'Biznes lanç' })
    await userEvent.click(within(lunch).getByRole('button', { name: /Kombo/ }))
    await userEvent.type(screen.getByLabelText('Ad'), 'Lanç 2')
    await userEvent.type(screen.getByLabelText('Qiymət (AZN)'), '15')
    const save = screen.getByRole('button', { name: 'Yadda saxla' })
    expect(save).toBeDisabled()
    // An existing combo is not offered as a component.
    expect(
      screen.queryByRole('button', { name: 'Lanç 1 artır' }),
    ).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Sezar artır' }))
    await userEvent.click(screen.getByRole('button', { name: 'Sezar artır' }))
    await userEvent.click(save)

    await screen.findByRole('region', { name: 'Biznes lanç' })
    expect(sent).toEqual({
      categoryId: 'c3',
      name: 'Lanç 2',
      description: null,
      price: 15,
      components: [{ itemId: 'd1', quantity: 2 }],
    })
  })

  it('explains why a dish inside a combo cannot be deleted (#138)', async () => {
    backend({
      [`DELETE ${MENU_URL}/items/d1`]: new Response(
        JSON.stringify({ status: 409 }),
        { status: 409 },
      ),
    })
    renderWithProviders(<MenuPage />)

    await userEvent.click(
      await screen.findByRole('button', { name: 'Sezar sil' }),
    )
    await userEvent.click(screen.getByRole('button', { name: 'Sil' }))

    expect(
      await screen.findByText(/kombonun tərkibindədir/),
    ).toBeInTheDocument()
  })

  it('explains why a category with dishes cannot be deleted', async () => {
    backend({
      [`DELETE ${MENU_URL}/categories/c1`]: new Response(
        JSON.stringify({ status: 409 }),
        { status: 409 },
      ),
    })
    renderWithProviders(<MenuPage />)

    await userEvent.click(
      await screen.findByRole('button', {
        name: 'Salatlar kateqoriyasını sil',
      }),
    )
    await userEvent.click(screen.getByRole('button', { name: 'Sil' }))

    expect(
      await screen.findByText(/Bu kateqoriyada məhsul var/),
    ).toBeInTheDocument()
  })

  it('shows a waiter the menu without edit buttons', async () => {
    signInAs('WAITER')
    backend()
    renderWithProviders(<MenuPage />)

    await screen.findByRole('heading', { name: 'Salatlar' })
    expect(
      screen.queryByRole('button', { name: /Kateqoriya əlavə et/ }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Sezar redaktə et' }),
    ).not.toBeInTheDocument()
  })
})
