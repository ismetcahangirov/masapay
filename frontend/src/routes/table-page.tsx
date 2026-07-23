import { useMemo } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { validateTableToken } from '@/lib/table-session'
import { InvalidTokenPage } from '@/routes/invalid-token-page'
import { BillCard } from '@/features/order/bill-card'
import { mockOrder } from '@/features/order/mock'

// Landing page reached by scanning a table QR code:
//   /r/:restaurantSlug/t/:tableId?token=<token>
//
// This route establishes the token gate (4.1) and renders the live-bill UI
// (4.4). Data fetching (4.2) and real-time sync (4.3) replace the mock order in
// later sub-issues.
export function TablePage() {
  const { restaurantSlug = '', tableId = '' } = useParams()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')

  const validation = useMemo(() => validateTableToken(token), [token])

  // Swapped for the RTK Query order feed in sub-issue 4.2.
  const order = useMemo(
    () => mockOrder(restaurantSlug, tableId),
    [restaurantSlug, tableId],
  )

  if (!validation.valid) {
    return <InvalidTokenPage reason={validation.reason} />
  }

  return (
    <main className="flex min-h-svh items-center justify-center p-6">
      <BillCard order={order} />
    </main>
  )
}
