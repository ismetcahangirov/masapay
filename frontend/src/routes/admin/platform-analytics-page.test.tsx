import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { api } from '@/app/api'
import { userLoaded } from '@/features/auth/auth-slice'
import { PlatformAnalyticsPage } from '@/routes/admin/platform-analytics-page'
import { mockApi, renderWithProviders, requestsOf } from '@/test/utils'

const URL = '/api/admin/platform/analytics'

const analytics = {
  from: '2026-09-24',
  to: '2026-10-23',
  timeZone: 'Asia/Baku',
  totals: [
    {
      currency: 'AZN',
      restaurantCount: 2,
      activeRestaurantCount: 1,
      paymentCount: 3,
      revenue: 100,
      tips: 10,
      subscriptionRevenue: 30,
    },
  ],
  restaurants: [
    {
      id: 'r1',
      name: 'Dolma',
      currency: 'AZN',
      active: true,
      subscriptionStatus: 'ACTIVE',
      paymentCount: 3,
      revenue: 100,
      tips: 10,
      averagePayment: 33.33,
      subscriptionRevenue: 30,
    },
    {
      id: 'r2',
      name: 'Nərgiz Kafe',
      currency: 'AZN',
      active: false,
      subscriptionStatus: 'EXPIRED',
      paymentCount: 0,
      revenue: 0,
      tips: 0,
      averagePayment: null,
      subscriptionRevenue: 0,
    },
  ],
}

describe('PlatformAnalyticsPage (#149)', () => {
  beforeEach(() => {
    vi.useFakeTimers({
      now: new Date('2026-10-23T08:00:00Z'),
      toFake: ['Date'],
    })
    store.dispatch(api.util.resetApiState())
    store.dispatch(
      userLoaded({ id: 'a1', email: 'admin@masapay.az', role: 'SUPER_ADMIN' }),
    )
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('shows the totals and compares the restaurants over the last 30 days', async () => {
    const fetchMock = mockApi({ [`GET ${URL}`]: analytics })
    renderWithProviders(<PlatformAnalyticsPage />)

    const totals = await screen.findByRole('region', { name: 'Cəmi, AZN' })
    expect(within(totals).getByText('1 / 2')).toBeInTheDocument()
    expect(within(totals).getByText('3')).toBeInTheDocument()

    const dolma = screen.getByText('Dolma').closest('tr') as HTMLElement
    expect(within(dolma).getByText('Davam edir')).toBeInTheDocument()
    const kafe = screen.getByText('Nərgiz Kafe').closest('tr') as HTMLElement
    expect(within(kafe).getByText('Bitib')).toBeInTheDocument()
    expect(within(kafe).getByText('Deaktiv')).toBeInTheDocument()
    expect(within(kafe).getByText('-')).toBeInTheDocument()

    expect(requestsOf(fetchMock)).toContain(
      `GET ${URL}?from=2026-09-24&to=2026-10-23`,
    )
  })

  it('asks again for another period', async () => {
    const fetchMock = mockApi({ [`GET ${URL}`]: analytics })
    renderWithProviders(<PlatformAnalyticsPage />)
    await screen.findByText('Dolma')

    await userEvent.click(screen.getByRole('radio', { name: 'Son 7 gün' }))

    await waitFor(() =>
      expect(requestsOf(fetchMock)).toContain(
        `GET ${URL}?from=2026-10-17&to=2026-10-23`,
      ),
    )
  })
})
