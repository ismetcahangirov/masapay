import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import { store } from '@/app/store'
import { api } from '@/app/api'
import { userLoaded } from '@/features/auth/auth-slice'
import { MyTipsPage } from '@/routes/admin/my-tips-page'
import { mockApi, renderWithProviders, requestsOf } from '@/test/utils'

describe('MyTipsPage (#182)', () => {
  beforeEach(() => {
    vi.useFakeTimers({
      now: new Date('2026-10-23T08:00:00Z'),
      toFake: ['Date'],
    })
    store.dispatch(api.util.resetApiState())
    store.dispatch(
      userLoaded({ id: 'w1', email: 'waiter@dolma.local', role: 'WAITER' }),
    )
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('shows the waiter their tips this month with the table of each', async () => {
    const fetchMock = mockApi({
      'GET /api/admin/me/tips': {
        from: '2026-10-01',
        to: '2026-10-23',
        timeZone: 'Asia/Baku',
        currency: 'AZN',
        tipCount: 2,
        tipTotal: 5.5,
        tips: [
          {
            transactionId: 't1',
            paidAt: '2026-10-22T18:00:00Z',
            tableLabel: 'Masa 4',
            tipAmount: 3,
            billAmount: 30,
          },
          {
            transactionId: 't2',
            paidAt: '2026-10-20T18:00:00Z',
            tableLabel: 'Masa 1',
            tipAmount: 2.5,
            billAmount: 25,
          },
        ],
      },
    })
    renderWithProviders(<MyTipsPage />)

    const masa4 = (await screen.findByText('Masa 4')).closest(
      'tr',
    ) as HTMLElement
    expect(within(masa4).getByText(/3,00|3\.00/)).toBeInTheDocument()
    expect(screen.getByText('Masa 1')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(requestsOf(fetchMock)).toContain(
      'GET /api/admin/me/tips?from=2026-10-01&to=2026-10-23',
    )
  })
})
