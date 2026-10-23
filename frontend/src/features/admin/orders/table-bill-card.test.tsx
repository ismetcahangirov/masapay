import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
  JUST_READY_MS,
  TableBillCard,
} from '@/features/admin/orders/table-bill-card'
import type { KitchenStatus, Order } from '@/features/order/types'

function order(sezar: KitchenStatus, dolma: KitchenStatus): Order {
  const line = (id: string, name: string, kitchenStatus: KitchenStatus) => ({
    id,
    name,
    quantity: 1,
    unitPrice: 850,
    total: 850,
    paid: false,
    kitchenStatus,
    source: 'CUSTOMER' as const,
    addedBy: null,
    addedAt: null,
  })
  return {
    id: 'o1',
    status: 'open',
    currency: 'AZN',
    totalAmount: 1700,
    paidAmount: 0,
    equalSplit: null,
    serviceCharge: null,
    items: [line('a', 'Sezar', sezar), line('b', 'Dolma', dolma)],
  } as unknown as Order
}

describe('TableBillCard ready dishes (#178)', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('counts ready dishes and highlights the one that just turned ready', () => {
    vi.useFakeTimers()
    const { rerender } = render(
      <TableBillCard tableLabel="Masa 4" order={order('READY', 'PREPARING')} />,
    )
    expect(screen.getByText('1 yemək hazırdır')).toBeInTheDocument()
    // Ready before the screen opened: not highlighted.
    expect(screen.getByText(/Sezar/).closest('li')).not.toHaveAttribute(
      'data-just-ready',
    )

    rerender(
      <TableBillCard tableLabel="Masa 4" order={order('READY', 'READY')} />,
    )
    expect(screen.getByText('2 yemək hazırdır')).toBeInTheDocument()
    expect(screen.getByText(/Dolma/).closest('li')).toHaveAttribute(
      'data-just-ready',
    )

    act(() => {
      vi.advanceTimersByTime(JUST_READY_MS)
    })
    expect(screen.getByText(/Dolma/).closest('li')).not.toHaveAttribute(
      'data-just-ready',
    )
  })
})

describe('TableBillCard waiter (#182)', () => {
  it('shows who serves the table and lets a waiter take it', async () => {
    const onClaim = vi.fn()
    const { rerender } = render(
      <TableBillCard
        tableLabel="Masa 4"
        order={order('NEW', 'NEW')}
        onClaim={onClaim}
      />,
    )
    expect(screen.getByText('Ofisiant yoxdur')).toBeInTheDocument()
    await userEvent.click(
      screen.getByRole('button', { name: 'Masa 4 masanı götür' }),
    )
    expect(onClaim).toHaveBeenCalledOnce()

    rerender(
      <TableBillCard
        tableLabel="Masa 4"
        order={{ ...order('NEW', 'NEW'), waiter: { id: 'w1', name: 'Aysel' } }}
      />,
    )
    expect(screen.getByText('Ofisiant: Aysel')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /masanı götür/ })).toBeNull()
  })
})
