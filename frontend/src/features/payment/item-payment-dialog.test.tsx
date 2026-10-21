import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ItemPaymentDialog } from '@/features/payment/item-payment-dialog'
import type { Order } from '@/features/order/types'
import { mockApi, renderWithProviders } from '@/test/utils'

const goToPayment = vi.hoisted(() => vi.fn())
vi.mock('@/features/payment/payment-api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/features/payment/payment-api')>()),
  goToPayment,
}))

const QR = '11111111-0000-0000-0000-000000000003'
const order: Order = {
  id: 'o1',
  status: 'partially_paid',
  currency: 'AZN',
  totalAmount: 2700,
  paidAmount: 900,
  equalSplit: null,
  serviceCharge: null,
  items: [
    {
      id: 'kabab',
      name: 'Lülə kabab',
      quantity: 1,
      unitPrice: 1800,
      total: 1800,
      paid: false,
    },
    {
      id: 'sorba',
      name: 'Mərci şorbası',
      quantity: 1,
      unitPrice: 900,
      total: 900,
      paid: true,
    },
  ],
}

describe('ItemPaymentDialog (Öz yediyim)', () => {
  it('pays only the picked lines; lines paid by someone else cannot be picked', async () => {
    let checkoutBody: unknown
    mockApi({
      [`GET /api/public/tables/${QR}/payment/items`]: {
        amount: 18,
        currency: 'AZN',
      },
      [`POST /api/public/tables/${QR}/payment/items/checkout`]: async (
        request: Request,
      ) => {
        checkoutBody = await request.json()
        return { transactionId: 'tx-2', paymentUrl: 'https://pay.test/tx-2' }
      },
    })
    renderWithProviders(
      <ItemPaymentDialog
        qrToken={QR}
        order={order}
        open
        onOpenChange={() => {}}
      />,
    )

    expect(screen.getByRole('button', { name: /Mərci şorbası/ })).toBeDisabled()
    const payButton = screen.getByRole('button', { name: 'Ödə' })
    expect(payButton).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: /Lülə kabab/ }))
    expect(
      await screen.findByRole('button', { name: /18,00.*ödə/ }),
    ).toBeEnabled()

    await userEvent.click(screen.getByRole('button', { name: /ödə$/ }))

    await waitFor(() => expect(goToPayment).toHaveBeenCalled())
    expect(checkoutBody).toEqual({ itemIds: ['kabab'], guests: null })
  })
})
