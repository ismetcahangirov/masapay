import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  ChefHat,
  CircleCheckBig,
  Clock,
  CookingPot,
  LoaderCircle,
  Undo2,
} from 'lucide-react'
import {
  type KitchenLine,
  type KitchenStatus,
  useKitchenWorkQuery,
  useMarkKitchenLineMutation,
} from '@/features/admin/kitchen/kitchen-api'
import { minutesSince, toTickets } from '@/features/admin/kitchen/tickets'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { cn } from '@/lib/utils'

// Keys in the "orders" namespace, translated at render.
const STATUS_LABELS = {
  NEW: 'kitchen.status.new',
  PREPARING: 'kitchen.status.preparing',
  READY: 'kitchen.status.ready',
} as const satisfies Record<KitchenStatus, string>

// Kitchen screen (#142): what customers ordered, one ticket per table, live.
// A new order appears without a reload; the chef moves each dish from new to
// preparing to ready, and every kitchen screen and the table see it.
export function KitchenPage() {
  const { t } = useTranslation('orders')
  const restaurants = useMyRestaurantsQuery()
  const [chosen, setChosen] = useState<string | null>(null)
  const restaurantId = chosen ?? restaurants.data?.[0]?.id ?? null

  const work = useKitchenWorkQuery(restaurantId ?? '', { skip: !restaurantId })
  const [mark] = useMarkKitchenLineMutation()

  // Waiting times tick on without a reload.
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30000)
    return () => window.clearInterval(timer)
  }, [])

  const tickets = toTickets(work.data ?? [])

  function setStatus(line: KitchenLine, status: KitchenStatus) {
    if (!restaurantId) return
    void mark({ restaurantId, lineId: line.id, status })
  }

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-4 sm:p-6">
      <header className="space-y-1">
        <h1 className="flex items-center gap-2 text-2xl font-semibold tracking-tight">
          <ChefHat className="size-6" aria-hidden />
          {t('kitchen.title')}
        </h1>
        <p className="text-sm text-muted-foreground">{t('kitchen.subtitle')}</p>
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

      {work.isLoading && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label={t('loading')}
        />
      )}
      {work.isError && (
        <p className="text-sm text-destructive">{t('kitchen.loadError')}</p>
      )}
      {work.data?.length === 0 && (
        <p className="text-sm text-muted-foreground">{t('kitchen.empty')}</p>
      )}

      <div className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,20rem),1fr))] gap-4">
        {tickets.map((ticket) => (
          <Card
            key={ticket.tableId}
            role="region"
            aria-label={ticket.tableLabel}
            className={cn(ticket.done && 'opacity-60')}
          >
            <CardHeader className="flex-row items-center justify-between gap-2 space-y-0 pb-3">
              <CardTitle className="text-lg">
                <h2>{ticket.tableLabel}</h2>
              </CardTitle>
              <span className="flex items-center gap-1 text-sm tabular-nums text-muted-foreground">
                <Clock className="size-4" aria-hidden />
                {t('kitchen.minutes', {
                  minutes: minutesSince(ticket.since, now),
                })}
              </span>
            </CardHeader>
            <CardContent>
              <ul className="divide-y">
                {ticket.lines.map((line) => (
                  <li
                    key={line.id}
                    className="flex items-center justify-between gap-3 py-2"
                  >
                    <div className="min-w-0">
                      <p
                        className={cn(
                          'font-medium',
                          line.status === 'READY' &&
                            'text-muted-foreground line-through',
                        )}
                      >
                        {line.quantity} x {line.name}
                      </p>
                      <Badge
                        variant={
                          line.status === 'READY'
                            ? 'success'
                            : line.status === 'PREPARING'
                              ? 'default'
                              : 'outline'
                        }
                      >
                        {t(STATUS_LABELS[line.status])}
                      </Badge>
                    </div>
                    {line.status === 'NEW' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setStatus(line, 'PREPARING')}
                        aria-label={t('kitchen.startLabel', {
                          name: line.name,
                        })}
                      >
                        <CookingPot aria-hidden />
                        {t('kitchen.start')}
                      </Button>
                    )}
                    {line.status === 'PREPARING' && (
                      <Button
                        size="sm"
                        variant="success"
                        onClick={() => setStatus(line, 'READY')}
                        aria-label={t('kitchen.readyLabel', {
                          name: line.name,
                        })}
                      >
                        <CircleCheckBig aria-hidden />
                        {t('kitchen.ready')}
                      </Button>
                    )}
                    {line.status === 'READY' && (
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setStatus(line, 'PREPARING')}
                        aria-label={t('kitchen.undoLabel', { name: line.name })}
                      >
                        <Undo2 aria-hidden />
                        {t('kitchen.undo')}
                      </Button>
                    )}
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}
