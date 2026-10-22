import { lazy, Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import { LandingPage } from '@/routes/landing-page'
import { TablePage } from '@/routes/table-page'
import { NotFoundPage } from '@/routes/not-found-page'
import { PaymentResultPage } from '@/routes/payment-result-page'
import { MockPayriffPage } from '@/routes/mock-payriff-page'
import { LoginPage } from '@/routes/admin/login-page'
import { RequireAuth } from '@/features/auth/require-auth'

// Admin panel is lazy-loaded so its charting bundle (recharts) stays out of the
// customer PWA's initial download.
const DashboardPage = lazy(() =>
  import('@/routes/admin/dashboard-page').then((m) => ({
    default: m.DashboardPage,
  })),
)
const TablesPage = lazy(() =>
  import('@/routes/admin/tables-page').then((m) => ({
    default: m.TablesPage,
  })),
)
const LiveTablesPage = lazy(() =>
  import('@/routes/admin/live-tables-page').then((m) => ({
    default: m.LiveTablesPage,
  })),
)
const TransactionsPage = lazy(() =>
  import('@/routes/admin/transactions-page').then((m) => ({
    default: m.TransactionsPage,
  })),
)
const SettingsPage = lazy(() =>
  import('@/routes/admin/settings-page').then((m) => ({
    default: m.SettingsPage,
  })),
)
const TipsPage = lazy(() =>
  import('@/routes/admin/tips-page').then((m) => ({
    default: m.TipsPage,
  })),
)
const MenuPage = lazy(() =>
  import('@/routes/admin/menu-page').then((m) => ({
    default: m.MenuPage,
  })),
)

export const router = createBrowserRouter([
  {
    path: '/',
    element: <LandingPage />,
  },
  {
    // Reached by scanning a table QR code.
    path: '/r/:restaurantSlug/t/:tableId',
    element: <TablePage />,
  },
  {
    // Payriff sends the guest back here after paying (#37).
    path: '/payment/result',
    element: <PaymentResultPage />,
  },
  {
    // Local stand-in for Payriff's page; only in development (#37).
    path: '/payment/mock',
    element: import.meta.env.DEV ? <MockPayriffPage /> : <NotFoundPage />,
  },
  {
    path: '/admin/login',
    element: <LoginPage />,
  },
  {
    // Admin panel: only signed-in managers and super admins (#17).
    path: '/admin',
    element: (
      <RequireAuth roles={['SUPER_ADMIN', 'RESTO_MANAGER']}>
        <Suspense fallback={null}>
          <DashboardPage />
        </Suspense>
      </RequireAuth>
    ),
  },
  {
    // Tables and QR codes: managers and waiters (#47).
    path: '/admin/tables',
    element: (
      <RequireAuth roles={['SUPER_ADMIN', 'RESTO_MANAGER', 'WAITER']}>
        <Suspense fallback={null}>
          <TablesPage />
        </Suspense>
      </RequireAuth>
    ),
  },
  {
    // Menu: managers edit it, waiters read it (#137).
    path: '/admin/menu',
    element: (
      <RequireAuth roles={['SUPER_ADMIN', 'RESTO_MANAGER', 'WAITER']}>
        <Suspense fallback={null}>
          <MenuPage />
        </Suspense>
      </RequireAuth>
    ),
  },
  {
    // Live floor plan: managers and waiters (#44).
    path: '/admin/live',
    element: (
      <RequireAuth roles={['SUPER_ADMIN', 'RESTO_MANAGER', 'WAITER']}>
        <Suspense fallback={null}>
          <LiveTablesPage />
        </Suspense>
      </RequireAuth>
    ),
  },
  {
    // Payments and refunds (#45).
    path: '/admin/transactions',
    element: (
      <RequireAuth roles={['SUPER_ADMIN', 'RESTO_MANAGER']}>
        <Suspense fallback={null}>
          <TransactionsPage />
        </Suspense>
      </RequireAuth>
    ),
  },
  {
    path: '/admin/settings',
    element: (
      <RequireAuth roles={['SUPER_ADMIN', 'RESTO_MANAGER']}>
        <Suspense fallback={null}>
          <SettingsPage />
        </Suspense>
      </RequireAuth>
    ),
  },
  {
    // Tip report per waiter with CSV export (#48).
    path: '/admin/tips',
    element: (
      <RequireAuth roles={['SUPER_ADMIN', 'RESTO_MANAGER']}>
        <Suspense fallback={null}>
          <TipsPage />
        </Suspense>
      </RequireAuth>
    ),
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
])
