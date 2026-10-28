import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { LoaderCircle, Radio, X } from 'lucide-react'
import {
  type FloorStatus,
  useRestaurantFloorQuery,
} from '@/features/admin/live/floor-api'
import {
  FLOOR_STATUS,
  FLOOR_STATUSES,
} from '@/features/admin/live/floor-status'
import { FloorTableTile } from '@/features/admin/live/floor-table-tile'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { cn } from '@/lib/utils'

// Live floor plan (#44): every table coloured by its bill status. The list is
// loaded once and then kept current over WebSocket, one table at a time. The
// status pills count the tables and filter them: press one to see only that
// status, press it again to see all.
export function LiveTablesPage() {
  const { t } = useTranslation('orders')
  const restaurants = useMyRestaurantsQuery()
  const [chosen, setChosen] = useState<string | null>(null)
  const [filter, setFilter] = useState<FloorStatus | null>(null)
  const restaurantId = chosen ?? restaurants.data?.[0]?.id ?? null

  const floor = useRestaurantFloorQuery(restaurantId ?? '', {
    skip: !restaurantId,
  })

  // A clock for the "open for N minutes" line; it ticks without a refetch.
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30000)
    return () => window.clearInterval(timer)
  }, [])

  const shown = filter
    ? floor.data?.filter((table) => table.status === filter)
    : floor.data

  const counts = FLOOR_STATUSES.map((status) => ({
    status,
    count: floor.data?.filter((table) => table.status === status).length ?? 0,
  }))

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-4 sm:p-6">
      <header className="space-y-1">
        <h1 className="flex items-center gap-2 text-2xl font-semibold tracking-tight">
          {t('live.title')}
          <Radio className="size-5 text-success" aria-hidden />
        </h1>
        <p className="text-sm text-muted-foreground">{t('live.subtitle')}</p>
      </header>

      {restaurants.data && restaurants.data.length > 1 && (
        <ToggleGroup
          type="single"
          value={restaurantId ?? undefined}
          onValueChange={(value) => value && setChosen(value)}
          className="flex-wrap justify-start"
        >
          {restaurants.data.map((restaurant) => (
            <ToggleGroupItem key={restaurant.id} value={restaurant.id}>
              {restaurant.name}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      )}

      {floor.data && floor.data.length > 0 && (
        <ul className="flex flex-wrap gap-2" aria-label={t('live.filterLabel')}>
          {counts.map(({ status, count }) => {
            const style = FLOOR_STATUS[status]
            const Icon = style.icon
            const active = filter === status
            return (
              <li key={status}>
                <button
                  type="button"
                  aria-pressed={active}
                  onClick={() => setFilter(active ? null : status)}
                  className={cn(
                    'inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-medium transition-shadow focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                    style.pill,
                    active && 'ring-2 ring-foreground ring-offset-2',
                    filter && !active && 'opacity-50 hover:opacity-100',
                  )}
                >
                  <Icon className="size-4" aria-hidden />
                  {t(style.label)}
                  <span className="tabular-nums">{count}</span>
                </button>
              </li>
            )
          })}
          {filter && (
            <li>
              <button
                type="button"
                onClick={() => setFilter(null)}
                className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-medium text-muted-foreground underline-offset-4 hover:underline"
              >
                <X className="size-4" aria-hidden />
                {t('live.showAll')}
              </button>
            </li>
          )}
        </ul>
      )}

      {floor.isLoading && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label={t('loading')}
        />
      )}
      {floor.isError && (
        <p className="text-sm text-destructive">{t('live.loadError')}</p>
      )}
      {floor.data?.length === 0 && (
        <p className="text-sm text-muted-foreground">{t('live.noTables')}</p>
      )}

      {filter && shown?.length === 0 && (
        <p className="text-sm text-muted-foreground">
          {t('live.noTablesInStatus')}
        </p>
      )}

      <div className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,14rem),1fr))] gap-4">
        {shown?.map((table) => (
          <FloorTableTile key={table.tableId} table={table} now={now} />
        ))}
      </div>
    </div>
  )
}
