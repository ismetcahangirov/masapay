import { beforeAll, beforeEach, describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { userLoaded } from '@/features/auth/auth-slice'
import { StaffPage } from '@/routes/admin/staff-page'
import { mockApi, renderWithProviders } from '@/test/utils'

const STAFF_URL = '/api/admin/restaurants/r1/staff'

const staff = [
  {
    id: 'u1',
    email: 'aysel@gmail.com',
    name: 'Aysel',
    role: 'WAITER',
    status: 'PENDING',
    lastLoginAt: '2026-10-22T09:00:00Z',
  },
  {
    id: 'u2',
    email: 'murad@gmail.com',
    name: 'Murad',
    role: 'CHEF',
    status: 'ACTIVE',
    lastLoginAt: '2026-10-22T08:00:00Z',
  },
  {
    id: 'u3',
    email: 'yeni@gmail.com',
    name: null,
    role: 'WAITER',
    status: 'INVITED',
    lastLoginAt: null,
  },
]

describe('StaffPage (#145)', () => {
  beforeAll(() => {
    // Radix Select uses pointer capture and scrolling that jsdom lacks.
    Element.prototype.hasPointerCapture ??= () => false
    Element.prototype.releasePointerCapture ??= () => {}
    Element.prototype.scrollIntoView ??= () => {}
  })

  beforeEach(() => {
    store.dispatch(
      userLoaded({ id: 'm1', email: 'm@dolma.az', role: 'RESTO_MANAGER' }),
    )
  })

  it('lists the staff with their state and the right action for each', async () => {
    mockApi({
      'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
      [`GET ${STAFF_URL}`]: staff,
    })
    renderWithProviders(<StaffPage />)

    const aysel = (await screen.findByText('Aysel')).closest(
      'tr',
    ) as HTMLElement
    expect(within(aysel).getByText('Təsdiq gözləyir')).toBeInTheDocument()
    expect(
      within(aysel).getByRole('button', { name: /təsdiqlə/ }),
    ).toBeInTheDocument()

    const murad = screen.getByText('Murad').closest('tr') as HTMLElement
    expect(within(murad).getByText('Aktiv')).toBeInTheDocument()
    expect(
      within(murad).getByRole('button', { name: /deaktiv et/ }),
    ).toBeInTheDocument()

    // Invited but never signed in: nothing to approve yet.
    const invited = screen
      .getByText('yeni@gmail.com')
      .closest('tr') as HTMLElement
    expect(within(invited).getByText(/hələ daxil olmayıb/)).toBeInTheDocument()
    expect(within(invited).queryByRole('button')).toBeNull()
  })

  it('invites a chef by email and approves a waiting waiter', async () => {
    const sent: unknown[] = []
    mockApi({
      'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
      [`GET ${STAFF_URL}`]: staff,
      [`POST ${STAFF_URL}`]: async (request: Request) => {
        sent.push(await request.json())
        return { ...staff[2], email: 'chef@gmail.com', role: 'CHEF' }
      },
      [`PATCH ${STAFF_URL}/u1`]: async (request: Request) => {
        sent.push(await request.json())
        return { ...staff[0], status: 'ACTIVE' }
      },
    })
    renderWithProviders(<StaffPage />)
    await screen.findByText('Aysel')

    await userEvent.type(
      screen.getByLabelText('Google emaili'),
      'chef@gmail.com',
    )
    await userEvent.click(screen.getByRole('combobox', { name: 'Rol' }))
    await userEvent.click(screen.getByRole('option', { name: 'Aşpaz' }))
    await userEvent.click(screen.getByRole('button', { name: 'Dəvət et' }))
    await waitFor(() =>
      expect(sent[0]).toEqual({ email: 'chef@gmail.com', role: 'CHEF' }),
    )
    expect(screen.getByLabelText('Google emaili')).toHaveValue('')

    await userEvent.click(
      screen.getByRole('button', { name: 'aysel@gmail.com təsdiqlə' }),
    )
    await waitFor(() => expect(sent[1]).toEqual({ active: true }))
  })

  it('says why an email cannot be invited', async () => {
    mockApi({
      'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
      [`GET ${STAFF_URL}`]: [],
      [`POST ${STAFF_URL}`]: new Response(JSON.stringify({ status: 409 }), {
        status: 409,
      }),
    })
    renderWithProviders(<StaffPage />)

    await userEvent.type(
      await screen.findByLabelText('Google emaili'),
      'boss@gmail.com',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Dəvət et' }))

    expect(
      await screen.findByText(/artıq bir işçiyə və ya başqa restorana aiddir/),
    ).toBeInTheDocument()
  })
})
