import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import { store } from '@/app/store'
import { api } from '@/app/api'
import { type UserRole, userLoaded } from '@/features/auth/auth-slice'
import { SubscriptionBanner } from '@/features/admin/subscription-banner'
import { settingsApi } from '@/features/admin/settings/settings-api'
import { mockApi, renderWithProviders } from '@/test/utils'
import i18n from '@/i18n'

function signIn(role: UserRole) {
  store.dispatch(userLoaded({ id: 'u1', email: 'm@dolma.local', role }))
}

function restaurant(subscriptionStatus: string, subscriptionEndsOn: string) {
  mockApi({
    'GET /api/admin/restaurants': [
      { id: 'r1', name: 'Dolma', subscriptionStatus, subscriptionEndsOn },
    ],
  })
}

describe('SubscriptionBanner (#148)', () => {
  beforeEach(() => {
    vi.useFakeTimers({
      now: new Date('2026-10-23T08:00:00Z'),
      toFake: ['Date'],
    })
    store.dispatch(api.util.resetApiState())
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('tells the manager that the subscription has ended', async () => {
    signIn('RESTO_MANAGER')
    restaurant('EXPIRED', '2026-10-20')
    renderWithProviders(<SubscriptionBanner />)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Abunəlik bitib (son gün 20.10.2026)',
    )
  })

  it('warns a week before the last day', async () => {
    signIn('RESTO_MANAGER')
    restaurant('ACTIVE', '2026-10-28')
    renderWithProviders(<SubscriptionBanner />)

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Abunəliyin bitməsinə 5 gün qalıb',
    )
  })

  it('warns in Russian with the right plural form (#215)', async () => {
    await i18n.changeLanguage('ru')
    signIn('RESTO_MANAGER')
    restaurant('ACTIVE', '2026-10-28')
    renderWithProviders(<SubscriptionBanner />)

    expect(await screen.findByRole('status')).toHaveTextContent(
      'До конца подписки осталось 5 дней (последний день 28.10.2026)',
    )
  })

  it('says nothing while the last day is far away', async () => {
    signIn('RESTO_MANAGER')
    restaurant('ACTIVE', '2026-12-31')
    const { container } = renderWithProviders(<SubscriptionBanner />)

    await vi.waitFor(() =>
      expect(
        settingsApi.endpoints.myRestaurants.select()(store.getState()).data,
      ).toHaveLength(1),
    )
    expect(container).toBeEmptyDOMElement()
  })

  it('is not shown to waiters, who do not pay for the platform', () => {
    signIn('WAITER')
    const fetchMock = mockApi({})
    const { container } = renderWithProviders(<SubscriptionBanner />)

    expect(container).toBeEmptyDOMElement()
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
