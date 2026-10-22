import { beforeEach, describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { store } from '@/app/store'
import { tokenReceived, userLoaded } from '@/features/auth/auth-slice'
import { RequireAuth } from '@/features/auth/require-auth'
import { KitchenPage } from '@/routes/admin/kitchen-page'
import { mockApi, renderWithProviders } from '@/test/utils'

const KITCHEN_URL = '/api/admin/restaurants/r1/kitchen'

const work = [
  {
    id: 'l1',
    tableId: 't4',
    tableLabel: 'Masa 4',
    name: 'Dolma',
    quantity: 2,
    status: 'NEW',
    orderedAt: '2026-10-22T12:00:00Z',
  },
  {
    id: 'l2',
    tableId: 't4',
    tableLabel: 'Masa 4',
    name: 'Ayran',
    quantity: 1,
    status: 'PREPARING',
    orderedAt: '2026-10-22T12:00:00Z',
  },
]

function backend(extra: Record<string, unknown> = {}) {
  return mockApi({
    'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
    [`GET ${KITCHEN_URL}`]: work,
    ...extra,
  })
}

describe('KitchenPage (#142)', () => {
  beforeEach(() => {
    store.dispatch(userLoaded({ id: 'u1', email: 'c@x.az', role: 'CHEF' }))
  })

  it('shows the confirmed dishes per table with their state', async () => {
    backend()
    renderWithProviders(<KitchenPage />)

    const ticket = await screen.findByRole('region', { name: 'Masa 4' })
    expect(within(ticket).getByText('2 x Dolma')).toBeInTheDocument()
    expect(within(ticket).getByText('Yeni')).toBeInTheDocument()
    expect(within(ticket).getByText('1 x Ayran')).toBeInTheDocument()
    expect(
      within(ticket).getByRole('button', { name: 'Ayran hazırdır' }),
    ).toBeInTheDocument()
  })

  it('moves a dish to preparing at once and saves it', async () => {
    let sent: unknown = null
    backend({
      [`PUT ${KITCHEN_URL}/items/l1`]: async (request: Request) => {
        sent = await request.json()
        return { ...work[0], status: 'PREPARING' }
      },
    })
    renderWithProviders(<KitchenPage />)

    await userEvent.click(
      await screen.findByRole('button', { name: 'Dolma hazırlanır' }),
    )

    expect(
      await screen.findByRole('button', { name: 'Dolma hazırdır' }),
    ).toBeInTheDocument()
    await waitFor(() => expect(sent).toEqual({ status: 'PREPARING' }))
  })

  it('sends a chef who opens the dashboard to the kitchen', async () => {
    store.dispatch(
      tokenReceived({
        accessToken: 'test-token',
        user: { id: 'u1', email: 'c@x.az', role: 'CHEF' },
      }),
    )
    backend()
    renderWithProviders(
      <Routes>
        <Route
          path="/admin"
          element={
            <RequireAuth roles={['SUPER_ADMIN', 'RESTO_MANAGER']}>
              <p>Dashboard</p>
            </RequireAuth>
          }
        />
        <Route path="/admin/kitchen" element={<p>Mətbəx ekranı</p>} />
      </Routes>,
      { route: '/admin' },
    )

    expect(await screen.findByText('Mətbəx ekranı')).toBeInTheDocument()
  })
})
