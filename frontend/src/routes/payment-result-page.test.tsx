import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { PaymentResultPage } from '@/routes/payment-result-page'
import { mockApi, renderWithProviders } from '@/test/utils'

function renderResult() {
  return renderWithProviders(
    <Routes>
      <Route path="/payment/result" element={<PaymentResultPage />} />
    </Routes>,
    { route: '/payment/result?tx=tx-1' },
  )
}

describe('PaymentResultPage (#37)', () => {
  it('shows success and links back to the table bill', async () => {
    mockApi({
      'GET /api/public/payments/tx-1': {
        transactionId: 'tx-1',
        status: 'SUCCESS',
        amount: 30,
        currency: 'AZN',
        orderStatus: 'PAID',
        restaurantId: 'r1',
        tableId: 't1',
        qrToken: 'q1',
      },
    })
    renderResult()

    expect(
      await screen.findByText('Ödəniş uğurla tamamlandı'),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Hesaba qayıt' })).toHaveAttribute(
      'href',
      '/r/r1/t/t1?token=q1',
    )
  })

  it('shows a declined payment as failed', async () => {
    mockApi({
      'GET /api/public/payments/tx-1': {
        transactionId: 'tx-1',
        status: 'FAILED',
        amount: 30,
        currency: 'AZN',
        orderStatus: 'OPEN',
        restaurantId: 'r1',
        tableId: 't1',
        qrToken: 'q1',
      },
    })
    renderResult()

    expect(await screen.findByText('Ödəniş baş tutmadı')).toBeInTheDocument()
  })

  it('tells the guest clearly when the connection is gone', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new TypeError('Failed to fetch')),
    )
    renderResult()

    expect(
      await screen.findByText('İnternet bağlantısı kəsildi'),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: /Yenidən yoxla/ }),
    ).toBeInTheDocument()
  })
})
