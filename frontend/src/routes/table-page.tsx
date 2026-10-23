import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { skipToken } from '@reduxjs/toolkit/query'
import { validateTableToken } from '@/lib/table-session'
import { InvalidTokenPage } from '@/routes/invalid-token-page'
import { BrandMark } from '@/components/layout/brand-mark'
import { BillCard } from '@/features/order/bill-card'
import {
  BillError,
  BillSkeleton,
  NoOpenBill,
} from '@/features/order/bill-states'
import { useGetTableOrderQuery } from '@/features/order/order-api'
import { MenuOrderCard } from '@/features/ordering/menu-order-card'
import { PaymentActions } from '@/features/payment/payment-actions'

// Landing page reached by scanning a table QR code:
//   /r/:restaurantId/t/:tableId?token=<qrToken>
//
// The token gate (4.1) runs first; the live bill then comes from RTK Query
// (4.2) with explicit loading, error and empty states.
export function TablePage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const validation = useMemo(() => validateTableToken(token), [token])

  const { data, error, isLoading, refetch } = useGetTableOrderQuery(
    validation.valid ? validation.qrToken : skipToken,
  )

  if (!validation.valid) {
    return <InvalidTokenPage reason={validation.reason} />
  }

  let content
  if (isLoading) {
    content = <BillSkeleton />
  } else if (error || !data) {
    const notFound = !!error && 'status' in error && error.status === 404
    content = <BillError notFound={notFound} onRetry={refetch} />
  } else {
    // Two cards (#140): ordering from the menu, then the bill and its payment.
    content = (
      <div className="flex w-full flex-col items-center gap-6">
        <MenuOrderCard qrToken={validation.qrToken} currency={data.currency} />
        <section
          aria-label="Ödəniş"
          className="flex w-full flex-col items-center gap-4"
        >
          {data.order ? (
            <>
              <BillCard table={data} order={data.order} />
              <PaymentActions qrToken={validation.qrToken} order={data.order} />
            </>
          ) : (
            <NoOpenBill
              tableLabel={data.tableLabel}
              restaurantName={data.restaurantName}
            />
          )}
        </section>
      </div>
    )
  }

  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-6 p-4 sm:p-6">
      {/* The brand above the customer's cards (#162 logo, single colour). */}
      <span className="flex items-center gap-2 text-lg tracking-tight">
        <BrandMark className="h-7" />
        masapay
      </span>
      {content}
    </main>
  )
}
