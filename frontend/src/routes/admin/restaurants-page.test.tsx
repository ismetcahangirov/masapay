import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { userLoaded } from '@/features/auth/auth-slice'
import { RestaurantsPage } from '@/routes/admin/restaurants-page'
import { mockApi, renderWithProviders } from '@/test/utils'

const URL = '/api/admin/platform/restaurants'

const restaurants = [
  {
    id: 'r1',
    name: 'Dolma',
    currency: 'AZN',
    posType: 'IIKO',
    active: true,
    createdAt: '2026-10-01T08:00:00Z',
    managers: [
      { id: 'm1', email: 'manager@dolma.local', name: 'Murad', signedIn: true },
    ],
    subscription: {
      status: 'EXPIRED',
      plan: 'BASIC',
      trial: false,
      startsOn: '2026-09-01',
      endsOn: '2026-10-01',
      monthlyFee: 49,
    },
  },
  {
    id: 'r2',
    name: 'Nərgiz Kafe',
    currency: 'AZN',
    posType: 'CUSTOM',
    active: false,
    createdAt: '2026-10-20T08:00:00Z',
    managers: [
      { id: 'm2', email: 'leyla@gmail.com', name: null, signedIn: false },
    ],
    subscription: { status: 'NONE', trial: false },
  },
]

describe('RestaurantsPage (#147, #148)', () => {
  beforeAll(() => {
    Element.prototype.hasPointerCapture ??= () => false
    Element.prototype.releasePointerCapture ??= () => {}
    Element.prototype.scrollIntoView ??= () => {}
  })

  beforeEach(() => {
    store.dispatch(
      userLoaded({ id: 'a1', email: 'admin@masapay.az', role: 'SUPER_ADMIN' }),
    )
  })

  it('lists restaurants with their managers and state', async () => {
    mockApi({ [`GET ${URL}`]: restaurants })
    renderWithProviders(<RestaurantsPage />)

    const dolma = (await screen.findByText('Dolma')).closest(
      'tr',
    ) as HTMLElement
    expect(within(dolma).getByText('iiko')).toBeInTheDocument()
    expect(within(dolma).getByText('Murad')).toBeInTheDocument()
    expect(within(dolma).getByText('Aktiv')).toBeInTheDocument()
    expect(within(dolma).getByText('Bitib')).toBeInTheDocument()
    expect(within(dolma).getByText('Son gün 01.10.2026')).toBeInTheDocument()

    const kafe = screen.getByText('Nərgiz Kafe').closest('tr') as HTMLElement
    expect(within(kafe).getByText('(hələ daxil olmayıb)')).toBeInTheDocument()
    expect(within(kafe).getByText('Deaktiv')).toBeInTheDocument()
    expect(within(kafe).getByText('Abunəlik yoxdur')).toBeInTheDocument()
  })

  it('opens a restaurant with its first manager', async () => {
    let sent: unknown = null
    mockApi({
      [`GET ${URL}`]: restaurants,
      [`POST ${URL}`]: async (request: Request) => {
        sent = await request.json()
        return restaurants[1]
      },
    })
    renderWithProviders(<RestaurantsPage />)
    await screen.findByText('Dolma')

    await userEvent.type(screen.getByLabelText('Restoranın adı'), 'Şəki Evi')
    await userEvent.click(screen.getByRole('combobox', { name: 'POS' }))
    await userEvent.click(screen.getByRole('option', { name: 'Micros' }))
    await userEvent.type(
      screen.getByLabelText('İlk menecerin Google emaili'),
      'boss@gmail.com',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Restoran aç' }))

    await waitFor(() =>
      expect(sent).toEqual({
        name: 'Şəki Evi',
        currency: 'AZN',
        posType: 'MICROS',
        managerEmail: 'boss@gmail.com',
      }),
    )
  })

  it('switches a restaurant off', async () => {
    let sent: unknown = null
    mockApi({
      [`GET ${URL}`]: restaurants,
      [`PATCH ${URL}/r1`]: async (request: Request) => {
        sent = await request.json()
        return { ...restaurants[0], active: false }
      },
    })
    renderWithProviders(<RestaurantsPage />)

    await userEvent.click(
      await screen.findByRole('button', { name: 'Dolma deaktiv et' }),
    )

    await waitFor(() => expect(sent).toEqual({ active: false }))
  })

  it('extends an expired subscription by a month from today', async () => {
    vi.useFakeTimers({
      now: new Date('2026-10-23T08:00:00Z'),
      toFake: ['Date'],
    })
    let sent: unknown = null
    mockApi({
      [`GET ${URL}`]: restaurants,
      [`PUT ${URL}/r1/subscription`]: async (request: Request) => {
        sent = await request.json()
        return restaurants[0]
      },
    })
    renderWithProviders(<RestaurantsPage />)

    await userEvent.click(
      await screen.findByRole('button', { name: 'Dolma abunəlik' }),
    )
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByLabelText('Son gün')).toHaveValue('2026-10-01')
    await userEvent.click(within(dialog).getByRole('button', { name: '+1 ay' }))
    await userEvent.click(within(dialog).getByRole('radio', { name: 'Pro' }))
    await userEvent.click(
      within(dialog).getByRole('button', { name: 'Yadda saxla' }),
    )

    await waitFor(() =>
      expect(sent).toEqual({
        plan: 'PRO',
        trial: false,
        startsOn: '2026-09-01',
        endsOn: '2026-11-23',
        monthlyFee: 49,
      }),
    )
    vi.useRealTimers()
  })

  it('refuses a last day before the first day', async () => {
    mockApi({ [`GET ${URL}`]: restaurants })
    renderWithProviders(<RestaurantsPage />)

    await userEvent.click(
      await screen.findByRole('button', { name: 'Dolma abunəlik' }),
    )
    const dialog = await screen.findByRole('dialog')
    const endsOn = within(dialog).getByLabelText('Son gün')
    await userEvent.clear(endsOn)
    await userEvent.type(endsOn, '2026-08-31')

    expect(
      within(dialog).getByText('Son gün başlama günündən əvvəl ola bilməz.'),
    ).toBeInTheDocument()
    expect(
      within(dialog).getByRole('button', { name: 'Yadda saxla' }),
    ).toBeDisabled()
  })
})
