import { beforeEach, describe, expect, it } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { api } from '@/app/api'
import { userLoaded } from '@/features/auth/auth-slice'
import { LiveTablesPage } from '@/routes/admin/live-tables-page'
import { mockApi, renderWithProviders } from '@/test/utils'

const floor = [
  {
    tableId: 't1',
    label: 'Masa 1',
    status: 'ACTIVE',
    currency: 'AZN',
    totalAmount: 33,
    paidAmount: 0,
    outstanding: 0,
    openedAt: '2026-10-25T08:00:00Z',
    minutesOpen: 60,
  },
  {
    tableId: 't2',
    label: 'Masa 2',
    status: 'ACTIVE',
    currency: 'AZN',
    totalAmount: 64.9,
    paidAmount: 0,
    outstanding: 0,
    openedAt: '2026-10-25T08:00:00Z',
    minutesOpen: 60,
  },
  {
    tableId: 't3',
    label: 'Masa 3',
    status: 'EMPTY',
    currency: 'AZN',
    totalAmount: null,
    paidAmount: null,
    outstanding: null,
    openedAt: null,
    minutesOpen: null,
  },
  {
    tableId: 't4',
    label: 'Masa 4',
    status: 'CLOSED',
    currency: 'AZN',
    totalAmount: 92.95,
    paidAmount: 92.95,
    outstanding: 0,
    openedAt: '2026-10-25T08:00:00Z',
    minutesOpen: 60,
  },
]

describe('LiveTablesPage status filter', () => {
  beforeEach(() => {
    store.dispatch(api.util.resetApiState())
    store.dispatch(
      userLoaded({ id: 'm1', email: 'm@x.az', role: 'RESTO_MANAGER' }),
    )
    mockApi({
      'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
      'GET /api/admin/restaurants/r1/floor': floor,
    })
  })

  it('shows only the tables of the pressed status, and all again on a second press', async () => {
    renderWithProviders(<LiveTablesPage />)
    await screen.findByText('Masa 1')
    const pills = screen.getByRole('list', { name: 'Status üzrə filtr' })
    const active = within(pills).getByRole('button', { name: /Aktiv/ })

    await userEvent.click(active)
    expect(active).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('Masa 1')).toBeInTheDocument()
    expect(screen.getByText('Masa 2')).toBeInTheDocument()
    expect(screen.queryByText('Masa 3')).toBeNull()
    expect(screen.queryByText('Masa 4')).toBeNull()

    await userEvent.click(active)
    expect(active).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByText('Masa 4')).toBeInTheDocument()
  })

  it('says so when no table has the pressed status, and resets with Hamısını göstər', async () => {
    renderWithProviders(<LiveTablesPage />)
    await screen.findByText('Masa 1')
    const pills = screen.getByRole('list', { name: 'Status üzrə filtr' })

    await userEvent.click(
      within(pills).getByRole('button', { name: /Qismən ödənilib/ }),
    )
    expect(screen.getByText('Bu statusda masa yoxdur.')).toBeInTheDocument()
    expect(screen.queryByText('Masa 1')).toBeNull()

    await userEvent.click(
      screen.getByRole('button', { name: 'Hamısını göstər' }),
    )
    expect(screen.getByText('Masa 1')).toBeInTheDocument()
    expect(screen.getByText('Masa 3')).toBeInTheDocument()
  })
})
