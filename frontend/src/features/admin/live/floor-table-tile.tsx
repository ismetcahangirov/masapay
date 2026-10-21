import type { FloorTable } from '@/features/admin/live/floor-api'
import { FLOOR_STATUS, formatMinutes } from '@/features/admin/live/floor-status'
import { formatCurrency } from '@/features/order/format'
import { toMinor } from '@/features/order/order-api'
import { cn } from '@/lib/utils'

/** Minutes the bill has been open at `now`, null when it is not open. */
function openMinutes(table: FloorTable, now: number): number | null {
  if (table.minutesOpen === null) return null
  if (!table.openedAt) return table.minutesOpen
  const opened = Date.parse(table.openedAt)
  if (Number.isNaN(opened)) return table.minutesOpen
  return Math.max(0, Math.floor((now - opened) / 60000))
}

/** One table on the live floor plan, coloured by its bill status. */
export function FloorTableTile({
  table,
  now,
}: {
  table: FloorTable
  now: number
}) {
  const style = FLOOR_STATUS[table.status]
  const Icon = style.icon
  const minutes = openMinutes(table, now)
  const money = (amount: number | null) =>
    formatCurrency(toMinor(amount ?? 0), table.currency)

  return (
    <article
      className={cn(
        'flex min-h-36 flex-col justify-between gap-3 rounded-lg border-2 p-4 transition-colors',
        style.tile,
      )}
      aria-label={`${table.label}: ${style.label}`}
    >
      <div className="flex items-start justify-between gap-2">
        <h2 className="text-lg font-semibold">{table.label}</h2>
        <span
          className={cn(
            'inline-flex shrink-0 items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium',
            style.pill,
          )}
        >
          <Icon className="size-3.5" aria-hidden />
          {style.label}
        </span>
      </div>

      {table.status === 'EMPTY' ? (
        <p className="text-sm">Açıq hesab yoxdur</p>
      ) : (
        <dl className="space-y-1 text-sm">
          <div className="flex justify-between gap-2">
            <dt>Cəmi</dt>
            <dd className="font-medium tabular-nums">
              {money(table.totalAmount)}
            </dd>
          </div>
          {table.status !== 'CLOSED' && (
            <div className="flex justify-between gap-2">
              <dt>Qalıq</dt>
              <dd className="font-semibold tabular-nums">
                {money(table.outstanding)}
              </dd>
            </div>
          )}
          {minutes !== null && (
            <div className="flex justify-between gap-2">
              <dt>Açıqdır</dt>
              <dd className="tabular-nums">{formatMinutes(minutes)}</dd>
            </div>
          )}
        </dl>
      )}
    </article>
  )
}
