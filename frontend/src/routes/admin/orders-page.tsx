import { useState } from 'react'
import { LoaderCircle } from 'lucide-react'
import { AdminNav } from '@/features/admin/admin-nav'
import { useRestaurantBillsQuery } from '@/features/admin/orders/orders-api'
import { TableBillCard } from '@/features/admin/orders/table-bill-card'
import { TableDetailDialog } from '@/features/admin/orders/table-detail-dialog'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

const ALL = 'ALL'

// Staff orders screen (#143): every table with a bill, live. Waiters see what
// each table ordered, where it is in the kitchen, what is paid and what is
// left; a table filter narrows it to one table. "Ətraflı" opens who added each
// line and when, the payments, and adding a dish from the menu (#144).
export function OrdersPage() {
  const restaurants = useMyRestaurantsQuery()
  const [chosen, setChosen] = useState<string | null>(null)
  const restaurantId = chosen ?? restaurants.data?.[0]?.id ?? null
  const [tableId, setTableId] = useState(ALL)
  const [detailId, setDetailId] = useState<string | null>(null)

  const bills = useRestaurantBillsQuery(restaurantId ?? '', {
    skip: !restaurantId,
  })
  const withBills = (bills.data ?? []).filter((table) => table.order)
  const shown =
    tableId === ALL
      ? withBills
      : withBills.filter((table) => table.tableId === tableId)

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-6">
      <AdminNav />
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">Sifarişlər</h1>
          <p className="text-sm text-muted-foreground">
            Masalarda nə sifariş olunub, nə ödənilib və nə qalıb. Canlı
            yenilənir.
          </p>
        </div>
        <div className="space-y-1">
          <Label htmlFor="orders-table">Masa</Label>
          <Select value={tableId} onValueChange={setTableId}>
            <SelectTrigger id="orders-table" className="w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>Bütün masalar</SelectItem>
              {withBills.map((table) => (
                <SelectItem key={table.tableId} value={table.tableId}>
                  {table.tableLabel}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </header>

      {restaurants.data && restaurants.data.length > 1 && (
        <ToggleGroup
          type="single"
          value={restaurantId ?? undefined}
          onValueChange={(value) => {
            if (!value) return
            setChosen(value)
            setTableId(ALL)
          }}
          className="flex-wrap justify-start"
        >
          {restaurants.data.map((restaurant) => (
            <ToggleGroupItem key={restaurant.id} value={restaurant.id}>
              {restaurant.name}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      )}

      {bills.isLoading && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label="Yüklənir"
        />
      )}
      {bills.isError && (
        <p className="text-sm text-destructive">
          Sifarişləri yükləmək alınmadı.
        </p>
      )}
      {bills.data && withBills.length === 0 && (
        <p className="text-sm text-muted-foreground">
          Hazırda açıq hesabı olan masa yoxdur.
        </p>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {shown.map(
          (table) =>
            table.order && (
              <TableBillCard
                key={table.tableId}
                tableLabel={table.tableLabel}
                order={table.order}
                onOpenDetail={() => setDetailId(table.tableId)}
              />
            ),
        )}
      </div>

      {restaurantId && (
        <TableDetailDialog
          restaurantId={restaurantId}
          // Looked up on every render, so a live update shows in the open dialog too.
          table={withBills.find((t) => t.tableId === detailId) ?? null}
          onClose={() => setDetailId(null)}
        />
      )}
    </div>
  )
}
