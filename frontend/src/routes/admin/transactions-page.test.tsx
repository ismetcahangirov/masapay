import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { userLoaded } from '@/features/auth/auth-slice'
import { TransactionsPage } from '@/routes/admin/transactions-page'
import { mockApi, renderWithProviders, requestsOf } from '@/test/utils'

const LIST = '/api/admin/restaurants/r1/transactions'

const page = {
  items: [
    {
      id: 'x1',
      createdAt: '2026-10-22T08:00:00Z',
      paidAt: '2026-10-22T08:01:00Z',
      tableLabel: 'Masa 1',
      splitType: 'FULL',
      status: 'SUCCESS',
      amount: 20,
      tipAmount: 2,
      totalAmount: 22,
      refundedAmount: 0,
      currency: 'AZN',
      payriffOrderId: 'PRF-1',
      refundable: true,
    },
  ],
  totalElements: 1,
  page: 0,
  size: 20,
}

describe('TransactionsPage Excel download (#170)', () => {
  const saved: string[] = []

  beforeAll(() => {
    // jsdom has no object URLs; the download is a link click.
    URL.createObjectURL = vi.fn(() => 'blob:report')
    URL.revokeObjectURL = vi.fn()
    Element.prototype.hasPointerCapture ??= () => false
    Element.prototype.releasePointerCapture ??= () => {}
    Element.prototype.scrollIntoView ??= () => {}
  })

  beforeEach(() => {
    store.dispatch(
      userLoaded({ id: 'm1', email: 'm@dolma.az', role: 'RESTO_MANAGER' }),
    )
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      saved.push(this.download)
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    saved.length = 0
  })

  it('downloads the filtered transactions under the name the server chose', async () => {
    const fetchMock = mockApi({
      'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
      [`GET ${LIST}`]: page,
      [`GET ${LIST}/export/xlsx`]: () =>
        new Response(new Blob(['xlsx']), {
          status: 200,
          headers: {
            'Content-Type':
              'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'Content-Disposition':
              'attachment; filename="dolma-emeliyyatlar.xlsx"; filename*=UTF-8\'\'dolma-emeliyyatlar.xlsx',
          },
        }),
    })
    renderWithProviders(<TransactionsPage />)
    await screen.findByText('Masa 1')

    await userEvent.click(screen.getByRole('combobox', { name: 'Status' }))
    await userEvent.click(screen.getByRole('option', { name: 'Uğurlu' }))
    await userEvent.click(screen.getByRole('button', { name: 'Excel yüklə' }))

    await waitFor(() => expect(saved).toEqual(['dolma-emeliyyatlar.xlsx']))
    expect(requestsOf(fetchMock)).toContain(
      `GET ${LIST}/export/xlsx?status=SUCCESS`,
    )
  })

  it('has nothing to download when no transaction matches', async () => {
    mockApi({
      'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
      [`GET ${LIST}`]: { ...page, items: [], totalElements: 0 },
    })
    renderWithProviders(<TransactionsPage />)

    await screen.findByText('Heç bir əməliyyat tapılmadı.')
    expect(screen.getByRole('button', { name: 'Excel yüklə' })).toBeDisabled()
  })

  it('tells the manager why Payriff did not refund (past 30 days)', async () => {
    mockApi({
      'GET /api/admin/restaurants': [{ id: 'r1', name: 'Dolma' }],
      [`GET ${LIST}`]: page,
      'POST /api/admin/transactions/x1/refund': () =>
        new Response(
          JSON.stringify({ status: 409, detail: 'REFUND_WINDOW_EXPIRED' }),
          {
            status: 409,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
    })
    renderWithProviders(<TransactionsPage />)

    await userEvent.click(
      await screen.findByRole('button', { name: /Geri qaytar/ }),
    )
    const dialog = await screen.findByRole('dialog')
    await userEvent.click(
      Array.from(dialog.querySelectorAll('button')).find((b) =>
        /Geri qaytar/.test(b.textContent ?? ''),
      )!,
    )

    expect(
      await screen.findByText(
        /30 gündən çox keçib: Payriff pulu geri qaytarmağa icazə vermir/,
      ),
    ).toBeInTheDocument()
  })
})
