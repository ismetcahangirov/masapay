import { describe, expect, it } from 'vitest'
import type { KitchenLine, KitchenStatus } from './kitchen-api'
import { minutesSince, toTickets } from './tickets'

const line = (
  id: string,
  table: string,
  orderedAt: string,
  status: KitchenStatus = 'NEW',
): KitchenLine => ({
  id,
  tableId: table,
  tableLabel: `Masa ${table}`,
  name: id,
  quantity: 1,
  status,
  orderedAt,
})

describe('kitchen tickets (#142)', () => {
  it('puts the longest-waiting table first and finished tables last', () => {
    const tickets = toTickets([
      line('a', '1', '2026-10-22T12:10:00Z'),
      line('b', '2', '2026-10-22T12:05:00Z'),
      line('c', '3', '2026-10-22T12:00:00Z', 'READY'),
      line('d', '1', '2026-10-22T12:01:00Z', 'READY'),
    ])

    expect(tickets.map((t) => t.tableLabel)).toEqual([
      'Masa 2',
      'Masa 1',
      'Masa 3',
    ])
    // Masa 1 waits since its oldest dish that is not ready yet.
    expect(tickets[1].since).toBe('2026-10-22T12:10:00Z')
    expect(tickets[1].lines.map((l) => l.id)).toEqual(['d', 'a'])
    expect(tickets[2].done).toBe(true)
  })

  it('counts whole minutes and never goes below zero', () => {
    const now = Date.parse('2026-10-22T12:10:59Z')
    expect(minutesSince('2026-10-22T12:00:00Z', now)).toBe(10)
    expect(minutesSince('2026-10-22T12:20:00Z', now)).toBe(0)
  })
})
