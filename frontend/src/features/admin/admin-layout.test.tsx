import { beforeEach, describe, expect, it } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { api } from '@/app/api'
import { type UserRole, userLoaded } from '@/features/auth/auth-slice'
import { AdminLayout } from '@/features/admin/admin-layout'
import { sidebarWasOpen } from '@/features/admin/sidebar-state'
import { mockApi, renderWithProviders } from '@/test/utils'

function signIn(role: UserRole) {
  store.dispatch(userLoaded({ id: 'u1', email: 'someone@masapay.az', role }))
}

function links(): string[] {
  const nav = screen.getByRole('navigation', { name: 'Admin naviqasiyası' })
  return within(nav)
    .getAllByRole('link')
    .map((link) => link.textContent ?? '')
}

describe('AdminLayout sidebar (#184)', () => {
  beforeEach(() => {
    store.dispatch(api.util.resetApiState())
    document.cookie = 'sidebar_state=; max-age=0; path=/'
    mockApi({})
  })

  it('shows the super admin the platform group and the restaurant pages', () => {
    signIn('SUPER_ADMIN')
    renderWithProviders(<AdminLayout>page</AdminLayout>, {
      route: '/admin/analytics',
    })

    expect(screen.getByText('Platforma')).toBeInTheDocument()
    expect(links()).toEqual(
      expect.arrayContaining(['Restoranlar', 'Analitika', 'Dashboard']),
    )
    expect(screen.getByRole('link', { name: 'Analitika' })).toHaveAttribute(
      'data-active',
      'true',
    )
    expect(screen.getByText('page')).toBeInTheDocument()
  })

  it('shows a waiter only the pages a waiter may open', () => {
    signIn('WAITER')
    renderWithProviders(<AdminLayout>page</AdminLayout>, {
      route: '/admin/orders',
    })

    expect(screen.queryByText('Platforma')).toBeNull()
    expect(links()).toEqual([
      'masapay',
      'Canlı masalar',
      'Masalar',
      'Sifarişlər',
      'Menyu',
    ])
  })

  it('collapses to icons and remembers it', async () => {
    signIn('RESTO_MANAGER')
    const { container } = renderWithProviders(<AdminLayout>page</AdminLayout>, {
      route: '/admin',
    })
    const sidebar = () => container.querySelector('[data-state]')

    expect(sidebar()).toHaveAttribute('data-state', 'expanded')
    await userEvent.click(
      screen.getByRole('button', { name: 'Menyunu aç / bağla' }),
    )

    expect(sidebar()).toHaveAttribute('data-state', 'collapsed')
    expect(document.cookie).toContain('sidebar_state=false')
    expect(sidebarWasOpen()).toBe(false)
  })

  it('opens the first time, with no cookie yet', () => {
    expect(sidebarWasOpen('')).toBe(true)
    expect(sidebarWasOpen('a=1; sidebar_state=true')).toBe(true)
    expect(sidebarWasOpen('sidebar_state=false; a=1')).toBe(false)
  })
})
