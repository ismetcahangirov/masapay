import { createBrowserRouter } from 'react-router-dom'
import App from '@/App'
import { TablePage } from '@/routes/table-page'
import { NotFoundPage } from '@/routes/not-found-page'

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
    path: '*',
    element: <NotFoundPage />,
  },
])
