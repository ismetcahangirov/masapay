import { useEffect, useState } from 'react'
import { LoaderCircle, Radio } from 'lucide-react'
import { useRestaurantFloorQuery } from '@/features/admin/live/floor-api'
import {
  FLOOR_STATUS,
  FLOOR_STATUSES,
} from '@/features/admin/live/floor-status'
import { FloorTableTile } from '@/features/admin/live/floor-table-tile'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { cn } from '@/lib/utils'

// Live floor plan (#44): every table coloured by its bill status. The list is
// loaded once and then kept current over WebSocket, one table at a time.
export function LiveTablesPage() {
  const restaurants = useMyRestaurantsQuery()
  const [chosen, setChosen] = useState<string | null>(null)
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

  const counts = FLOOR_STATUSES.map((status) => ({
    status,
    count: floor.data?.filter((table) => table.status === status).length ?? 0,
  }))

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-4 sm:p-6">
      <header className="space-y-1">
        <h1 className="flex items-center gap-2 text-2xl font-semibold tracking-tight">
          Canlı masalar
          <Radio className="size-5 text-success" aria-hidden />
        </h1>
        <p className="text-sm text-muted-foreground">
          Masaların vəziyyəti real vaxtda yenilənir: sifariş əlavə olunanda və
          ya ödəniş ediləndə rəng dərhal dəyişir.
        </p>
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
        <ul className="flex flex-wrap gap-2" aria-label="Status üzrə say">
          {counts.map(({ status, count }) => {
            const style = FLOOR_STATUS[status]
            const Icon = style.icon
            return (
              <li
                key={status}
                className={cn(
                  'inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-medium',
                  style.pill,
                )}
              >
                <Icon className="size-4" aria-hidden />
                {style.label}
                <span className="tabular-nums">{count}</span>
              </li>
            )
          })}
        </ul>
      )}

      {floor.isLoading && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label="Yüklənir"
        />
      )}
      {floor.isError && (
        <p className="text-sm text-destructive">Masaları yükləmək alınmadı.</p>
      )}
      {floor.data?.length === 0 && (
        <p className="text-sm text-muted-foreground">Hələ masa yoxdur.</p>
      )}

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {floor.data?.map((table) => (
          <FloorTableTile key={table.tableId} table={table} now={now} />
        ))}
      </div>
    </div>
  )
}
