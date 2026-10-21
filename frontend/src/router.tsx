import { lazy, Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import { LandingPage } from '@/routes/landing-page'
import { TablePage } from '@/routes/table-page'
import { NotFoundPage } from '@/routes/not-found-page'
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
const SettingsPage = lazy(() =>
  import('@/routes/admin/settings-page').then((m) => ({
    default: m.SettingsPage,
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
    path: '*',
    element: <NotFoundPage />,
  },
])
