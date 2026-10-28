import { beforeEach, describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { userLoaded } from '@/features/auth/auth-slice'
import { MenuPage } from '@/routes/admin/menu-page'
import { mockApi, renderWithProviders } from '@/test/utils'

const MENU_URL = '/api/admin/restaurants/r1/menu'

const sezar = {
  id: 'd1',
  categoryId: 'c1',
  name: 'Sezar',
  description: null,
  ingredients: null,
  price: 8,
  components: [],
  active: true,
  offOn: null,
  availableFrom: null,
  availableUntil: null,
  availableToday: true,
  suggestionIds: [],
  askBeforeConfirm: false,
  imageUrl: '/api/public/menu-images/d1?v=2',
}

const menu = [{ id: 'c1', name: 'Salatlar', sortOrder: 0, items: [sezar] }]

const draft = {
  categories: [
    {
      name: 'Salatlar',
      existing: true,
      items: [
        {
          name: 'Sezar',
          description: null,
          ingredients: null,
          price: 9,
          image: null,
          existingId: 'd1',
        },
        {
          name: 'Çoban',
          description: null,
          ingredients: 'pomidor, xiyar',
          price: 6.5,
          image: 'data:image/jpeg;base64,AAAA',
          existingId: null,
        },
      ],
    },
    {
      name: 'İçkilər',
      existing: false,
      items: [
        {
          name: 'Ayran',
          description: null,
          ingredients: null,
          price: 2,
          image: null,
          existingId: null,
        },
      ],
    },
  ],
  skippedImages: 1,
  ocr: false,
}

function backend(extra: Record<string, unknown> = {}) {
  return mockApi({
    'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
    [`GET ${MENU_URL}`]: menu,
    ...extra,
  })
}

const pdf = () =>
  new File(['%PDF-1.7'], 'menyu.pdf', { type: 'application/pdf' })

describe('menu import from a file', () => {
  beforeEach(() => {
    store.dispatch(
      userLoaded({ id: 'u1', email: 'u@x.az', role: 'RESTO_MANAGER' }),
    )
  })

  it('reads the file, lets the manager fix it, and saves only on approve', async () => {
    let sent: { categories: unknown[] } | null = null
    backend({
      [`POST ${MENU_URL}/import/preview`]: draft,
      [`POST ${MENU_URL}/import`]: async (request: Request) => {
        sent = (await request.json()) as { categories: unknown[] }
        return { categoriesCreated: 1, dishesCreated: 1, dishesUpdated: 1 }
      },
    })
    const user = userEvent.setup()
    renderWithProviders(<MenuPage />)

    await user.click(
      await screen.findByRole('button', { name: /Menyunu fayldan yüklə/ }),
    )
    await user.upload(screen.getByTestId('menu-file-input'), pdf())

    const dialog = await screen.findByRole('dialog')
    expect(
      await within(dialog).findByText(/1 şəkil oxunmadı/),
    ).toBeInTheDocument()
    expect(within(dialog).getByText('Mövcud kateqoriya')).toBeInTheDocument()
    expect(
      within(dialog).getByText('Menyuda var, yenilənəcək'),
    ).toBeInTheDocument()

    // The manager corrects a price and drops a dish they do not want.
    const prices = within(dialog).getAllByLabelText('Qiymət (AZN)')
    await user.clear(prices[1])
    await user.type(prices[1], '7,5')
    const ingredients = within(dialog).getAllByLabelText('Əsas tərkib')
    expect(ingredients[1]).toHaveValue('pomidor, xiyar')
    await user.type(ingredients[1], ', soğan')
    await user.type(within(dialog).getAllByLabelText('Təsvir')[1], 'Ev üsulu')
    await user.click(
      within(dialog).getByRole('button', { name: 'Ayran çıxar' }),
    )

    await user.click(
      within(dialog).getByRole('button', { name: 'Təsdiqlə (2 məhsul)' }),
    )

    await waitFor(() =>
      expect(sent).toEqual({
        categories: [
          {
            name: 'Salatlar',
            existing: true,
            items: [
              {
                name: 'Sezar',
                description: null,
                ingredients: null,
                price: 9,
                image: null,
                existingId: null,
              },
              {
                name: 'Çoban',
                description: 'Ev üsulu',
                ingredients: 'pomidor, xiyar, soğan',
                price: 7.5,
                image: 'data:image/jpeg;base64,AAAA',
                existingId: null,
              },
            ],
          },
        ],
      }),
    )
    expect(
      await screen.findByText(/1 məhsul əlavə olundu, 1 məhsul yeniləndi/),
    ).toBeInTheDocument()
  })

  it('does not approve two dishes with the same name', async () => {
    backend({ [`POST ${MENU_URL}/import/preview`]: draft })
    const user = userEvent.setup()
    renderWithProviders(<MenuPage />)

    await user.click(
      await screen.findByRole('button', { name: /Menyunu fayldan yüklə/ }),
    )
    await user.upload(screen.getByTestId('menu-file-input'), pdf())
    const dialog = await screen.findByRole('dialog')
    const names = await within(dialog).findAllByLabelText('Məhsulun adı')
    await user.clear(names[2])
    await user.type(names[2], 'sezar')

    expect(within(dialog).getAllByText('Bu ad iki dəfədir')).toHaveLength(2)
    expect(
      within(dialog).getByRole('button', { name: 'Təsdiqlə (3 məhsul)' }),
    ).toBeDisabled()
  })

  it('reads a photo of a printed menu and asks for a closer look at the OCR result', async () => {
    backend({
      [`POST ${MENU_URL}/import/preview`]: { ...draft, ocr: true },
    })
    const user = userEvent.setup()
    renderWithProviders(<MenuPage />)

    await user.click(
      await screen.findByRole('button', { name: /Menyunu fayldan yüklə/ }),
    )
    await user.upload(
      screen.getByTestId('menu-file-input'),
      new File(['jpeg'], 'menyu.jpg', { type: 'image/jpeg' }),
    )

    expect(
      await screen.findByText(/Yazılar şəkildən tanındı \(OCR\)/),
    ).toBeInTheDocument()
  })

  it.each([
    ['NO_TEXT', /oxunaqlı mətn tapılmadı/],
    ['OCR_UNAVAILABLE', /bu serverdə qurulmayıb/],
    ['OCR_TIMEOUT', /çox uzun çəkdi/],
  ])('explains %s', async (reason, message) => {
    backend({
      [`POST ${MENU_URL}/import/preview`]: new Response(
        JSON.stringify({ status: 422, detail: reason }),
        {
          status: 422,
          headers: { 'Content-Type': 'application/problem+json' },
        },
      ),
    })
    const user = userEvent.setup()
    renderWithProviders(<MenuPage />)

    await user.click(
      await screen.findByRole('button', { name: /Menyunu fayldan yüklə/ }),
    )
    await user.upload(screen.getByTestId('menu-file-input'), pdf())

    expect(await screen.findByText(message)).toBeInTheDocument()
  })
})

describe('dish photo', () => {
  beforeEach(() => {
    store.dispatch(
      userLoaded({ id: 'u1', email: 'u@x.az', role: 'RESTO_MANAGER' }),
    )
  })

  it('shows the photo on the menu and uploads a new one after saving the dish', async () => {
    // jsdom FormData does not survive Node Request in tests; the live check covers the body.
    let uploaded = false
    backend({
      [`PUT ${MENU_URL}/items/d1`]: sezar,
      [`PUT ${MENU_URL}/items/d1/image`]: () => {
        uploaded = true
        return { ...sezar, imageUrl: '/api/public/menu-images/d1?v=3' }
      },
    })
    const user = userEvent.setup()
    const { container } = renderWithProviders(<MenuPage />)

    await screen.findByText('Sezar')
    expect(container.querySelector('img')?.getAttribute('src')).toBe(
      '/api/public/menu-images/d1?v=2',
    )

    await user.click(screen.getByRole('button', { name: 'Sezar redaktə et' }))
    const dialog = await screen.findByRole('dialog')
    URL.createObjectURL = () => 'blob:photo'
    await user.upload(
      within(dialog).getByTestId('dish-photo-input'),
      new File(['jpeg'], 'sezar.jpg', { type: 'image/jpeg' }),
    )
    await user.click(
      within(dialog).getByRole('button', { name: 'Yadda saxla' }),
    )

    await waitFor(() => expect(uploaded).toBe(true))
  })
})
