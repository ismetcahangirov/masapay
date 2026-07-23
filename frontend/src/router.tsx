import { lazy, Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import App from '@/App'
import { TablePage } from '@/routes/table-page'
import { NotFoundPage } from '@/routes/not-found-page'

// Admin panel is lazy-loaded so its charting bundle (recharts) stays out of the
// customer PWA's initial download.
const DashboardPage = lazy(() =>
  import('@/routes/admin/dashboard-page').then((m) => ({
    default: m.DashboardPage,
  })),
)

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
  },
  {
    // Reached by scanning a table QR code.
    path: '/r/:restaurantSlug/t/:tableId',
    element: <TablePage />,
  },
  {
    // Admin panel. Role-based protection is added with the auth flow (#17).
    path: '/admin',
    element: (
      <Suspense fallback={null}>
        <DashboardPage />
      </Suspense>
    ),
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
])
