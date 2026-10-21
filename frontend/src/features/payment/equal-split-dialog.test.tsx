import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { EqualSplitDialog } from '@/features/payment/equal-split-dialog'
import type { Order } from '@/features/order/types'
import { mockApi, renderWithProviders, requestsOf } from '@/test/utils'

const goToPayment = vi.hoisted(() => vi.fn())
vi.mock('@/features/payment/payment-api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/features/payment/payment-api')>()),
  goToPayment,
}))

const QR = '11111111-0000-0000-0000-000000000001'
const order: Order = {
  id: 'o1',
  status: 'open',
  currency: 'AZN',
  totalAmount: 6000,
  paidAmount: 0,
  equalSplit: null,
  serviceCharge: null,
  items: [],
}

describe('EqualSplitDialog (Bərabər böl)', () => {
  it('quotes the share for the chosen number of people and starts the Payriff payment', async () => {
    const fetchMock = mockApi({
      [`GET /api/public/tables/${QR}/payment/equal`]: (request: Request) => {
        const parts = Number(new URL(request.url).searchParams.get('parts'))
        return {
          amount: 60 / parts,
          currency: 'AZN',
          totalParts: parts,
          paidParts: 0,
        }
      },
      [`POST /api/public/tables/${QR}/payment/equal/checkout`]: {
        transactionId: 'tx-1',
        paymentUrl: 'https://pay.test/tx-1',
      },
    })
    renderWithProviders(
      <EqualSplitDialog
        qrToken={QR}
        order={order}
        open
        onOpenChange={() => {}}
      />,
    )

    expect(
      await screen.findByRole('button', { name: /30,00.*ödə/ }),
    ).toBeEnabled()

    await userEvent.click(
      screen.getByRole('button', { name: 'Nəfər sayı: artır' }),
    )
    expect(
      await screen.findByRole('button', { name: /20,00.*ödə/ }),
    ).toBeEnabled()

    await userEvent.click(screen.getByRole('button', { name: /ödə$/ }))

    await waitFor(() =>
      expect(goToPayment).toHaveBeenCalledWith({
        transactionId: 'tx-1',
        paymentUrl: 'https://pay.test/tx-1',
      }),
    )
    expect(requestsOf(fetchMock)).toContain(
      `POST /api/public/tables/${QR}/payment/equal/checkout`,
    )
  })

  it('locks the people count once a split has started and explains why', async () => {
    mockApi({
      [`GET /api/public/tables/${QR}/payment/equal`]: {
        amount: 30,
        currency: 'AZN',
        totalParts: 2,
        paidParts: 1,
      },
    })
    renderWithProviders(
      <EqualSplitDialog
        qrToken={QR}
        order={{ ...order, equalSplit: { totalParts: 2, paidParts: 1 } }}
        open
        onOpenChange={() => {}}
      />,
    )

    expect(
      screen.getByRole('button', { name: 'Nəfər sayı: artır' }),
    ).toBeDisabled()
    expect(screen.getByText(/Bölgü başlayıb/)).toBeInTheDocument()
  })
})
