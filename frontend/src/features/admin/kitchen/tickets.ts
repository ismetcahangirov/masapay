import type { KitchenLine } from './kitchen-api'

// Kitchen lines grouped into one ticket per table (#142). Tables still
// waiting for food come first, longest-waiting first; tables whose dishes are
// all ready go to the end.

export interface KitchenTicket {
  tableId: string
  tableLabel: string
  /** When the oldest dish still being waited for was ordered (or the oldest one). */
  since: string
  lines: KitchenLine[]
  /** Every dish on it is ready. */
  done: boolean
}

export function toTickets(lines: KitchenLine[]): KitchenTicket[] {
  const byTable = new Map<string, KitchenLine[]>()
  for (const line of lines) {
    byTable.set(line.tableId, [...(byTable.get(line.tableId) ?? []), line])
  }
  const tickets = [...byTable.values()].map((tableLines): KitchenTicket => {
    const sorted = [...tableLines].sort((a, b) =>
      a.orderedAt.localeCompare(b.orderedAt),
    )
    const waiting = sorted.filter((line) => line.status !== 'READY')
    return {
      tableId: sorted[0].tableId,
      tableLabel: sorted[0].tableLabel,
      since: (waiting[0] ?? sorted[0]).orderedAt,
      lines: sorted,
      done: waiting.length === 0,
    }
  })
  return tickets.sort((a, b) =>
    a.done === b.done ? a.since.localeCompare(b.since) : a.done ? 1 : -1,
  )
}

/** Whole minutes from an ISO time until now, never negative. */
export function minutesSince(iso: string, now: number): number {
  return Math.max(0, Math.floor((now - new Date(iso).getTime()) / 60000))
}
