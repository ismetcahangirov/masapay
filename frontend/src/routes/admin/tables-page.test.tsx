import { describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { type UserRole, userLoaded } from '@/features/auth/auth-slice'
import { TablesPage } from '@/routes/admin/tables-page'
import { mockApi, renderWithProviders } from '@/test/utils'

const tables = [
  {
    id: 't1',
    label: 'VIP',
    status: 'AVAILABLE',
    qrToken: 'q1',
    customerUrl: 'http://localhost:5173/r/r1/t/t1?token=q1',
    serviceChargePercent: null,
    effectiveServiceChargePercent: 10,
  },
]

function backend(extra: Record<string, unknown> = {}) {
  return mockApi({
    'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
    'GET /api/admin/restaurants/r1/tables': tables,
    'GET /api/admin/tables/t1/qr': '<svg xmlns="http://www.w3.org/2000/svg"/>',
    ...extra,
  })
}

function signInAs(role: UserRole) {
  store.dispatch(userLoaded({ id: 'u1', email: 'u@x.az', role }))
}

describe('TablesPage service charge (#146)', () => {
  it('lets a manager give the VIP table its own 15%', async () => {
    signInAs('RESTO_MANAGER')
    let sent: unknown = null
    backend({
      'PUT /api/admin/restaurants/r1/tables/t1/service-charge': async (
        request: Request,
      ) => {
        sent = await request.json()
        return {
          ...tables[0],
          serviceChargePercent: 15,
          effectiveServiceChargePercent: 15,
        }
      },
    })
    renderWithProviders(<TablesPage />)

    expect(
      await screen.findByText('Xidmət haqqı: 10% (restoran)'),
    ).toBeInTheDocument()
    await userEvent.click(
      screen.getByRole('button', { name: 'VIP xidmət haqqı' }),
    )
    const dialog = screen.getByRole('dialog', { name: 'Xidmət haqqı' })
    await userEvent.click(
      within(dialog).getByRole('radio', { name: 'Xüsusi faiz' }),
    )
    await userEvent.type(within(dialog).getByLabelText('Faiz (%)'), '15')
    await userEvent.click(
      within(dialog).getByRole('button', { name: 'Yadda saxla' }),
    )

    await waitFor(() => expect(sent).toEqual({ percent: 15 }))
  })

  it('turns the charge off at one table with "Yoxdur"', async () => {
    signInAs('RESTO_MANAGER')
    let sent: unknown = null
    backend({
      'PUT /api/admin/restaurants/r1/tables/t1/service-charge': async (
        request: Request,
      ) => {
        sent = await request.json()
        return tables[0]
      },
    })
    renderWithProviders(<TablesPage />)

    await userEvent.click(
      await screen.findByRole('button', { name: 'VIP xidmət haqqı' }),
    )
    const dialog = screen.getByRole('dialog', { name: 'Xidmət haqqı' })
    await userEvent.click(within(dialog).getByRole('radio', { name: 'Yoxdur' }))
    await userEvent.click(
      within(dialog).getByRole('button', { name: 'Yadda saxla' }),
    )

    await waitFor(() => expect(sent).toEqual({ percent: 0 }))
  })

  it('shows a waiter the charge but no way to change it', async () => {
    signInAs('WAITER')
    backend()
    renderWithProviders(<TablesPage />)

    expect(
      await screen.findByText('Xidmət haqqı: 10% (restoran)'),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'VIP xidmət haqqı' }),
    ).not.toBeInTheDocument()
  })
})
