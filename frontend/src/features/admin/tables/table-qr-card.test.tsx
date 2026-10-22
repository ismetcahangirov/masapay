import { describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import { TableQrCard } from '@/features/admin/tables/table-qr-card'
import { mockApi, renderWithProviders } from '@/test/utils'

const svg =
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 29 29"><rect width="1" height="1"/></svg>'

describe('TableQrCard (QR)', () => {
  it('shows the table QR code from the backend and links to the customer page', async () => {
    mockApi({ 'GET /api/admin/tables/t7/qr': svg })
    renderWithProviders(
      <TableQrCard
        table={{
          id: 't7',
          label: 'Masa 7',
          status: 'AVAILABLE',
          qrToken: 'q7',
          customerUrl: 'http://localhost:5173/r/r1/t/t7?token=q7',
          serviceChargePercent: null,
          effectiveServiceChargePercent: 10,
        }}
      />,
    )

    const image = await screen.findByRole('img', { name: 'Masa 7 QR kodu' })
    expect(image.getAttribute('src')).toBe(
      `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`,
    )
    expect(screen.getByRole('link', { name: /Aç/ })).toHaveAttribute(
      'href',
      'http://localhost:5173/r/r1/t/t7?token=q7',
    )
    expect(screen.getByRole('button', { name: /SVG/ })).toBeEnabled()
  })
})
