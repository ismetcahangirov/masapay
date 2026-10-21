import { type FormEvent, useState } from 'react'
import { FolderDown, LoaderCircle, Plus } from 'lucide-react'
import { useAppSelector } from '@/app/hooks'
import { AdminNav } from '@/features/admin/admin-nav'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import {
  saveFile,
  useCreateTableMutation,
  useRestaurantQrZipMutation,
  useRestaurantTablesQuery,
} from '@/features/admin/tables/tables-api'
import { TableQrCard } from '@/features/admin/tables/table-qr-card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

// Tables and their QR codes (#47). Managers add tables and print the codes;
// waiters see the same codes (e.g. to reprint one) but cannot add tables.
export function TablesPage() {
  const role = useAppSelector((state) => state.auth.user?.role)
  const canCreate = role === 'SUPER_ADMIN' || role === 'RESTO_MANAGER'

  const restaurants = useMyRestaurantsQuery()
  const [chosen, setChosen] = useState<string | null>(null)
  const restaurantId = chosen ?? restaurants.data?.[0]?.id ?? null
  const restaurantName = restaurants.data?.find(
    (r) => r.id === restaurantId,
  )?.name

  const tables = useRestaurantTablesQuery(restaurantId ?? '', {
    skip: !restaurantId,
  })
  const [createTable, createState] = useCreateTableMutation()
  const [downloadZip, zipState] = useRestaurantQrZipMutation()
  const [label, setLabel] = useState('')

  async function onCreate(event: FormEvent) {
    event.preventDefault()
    if (!restaurantId || !label.trim()) return
    try {
      await createTable({ restaurantId, label: label.trim() }).unwrap()
      setLabel('')
    } catch {
      // createState.error is shown below.
    }
  }

  async function onDownloadAll() {
    if (!restaurantId) return
    try {
      const zip = await downloadZip(restaurantId).unwrap()
      saveFile(zip, `${restaurantName ?? 'masalar'}-qr-kodlar.zip`)
    } catch {
      // zipState.isError is shown below.
    }
  }

  const duplicate =
    createState.error &&
    'status' in createState.error &&
    createState.error.status === 409

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-6">
      <AdminNav />
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">Masalar</h1>
          <p className="text-sm text-muted-foreground">
            Hər masanın öz QR kodu var. Müştəri onu skan edib hesabı görür və
            ödəyir.
          </p>
        </div>
        <Button
          onClick={() => void onDownloadAll()}
          disabled={!tables.data?.length || zipState.isLoading}
        >
          <FolderDown aria-hidden />
          {zipState.isLoading ? 'Hazırlanır...' : 'Hamısını yüklə (ZIP)'}
        </Button>
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

      {canCreate && restaurantId && (
        <form
          onSubmit={(event) => void onCreate(event)}
          className="flex max-w-md gap-2"
        >
          <Input
            value={label}
            onChange={(event) => setLabel(event.target.value)}
            placeholder="Yeni masanın adı, məsələn Masa 7"
            maxLength={50}
            aria-label="Yeni masanın adı"
          />
          <Button
            type="submit"
            disabled={!label.trim() || createState.isLoading}
          >
            <Plus aria-hidden />
            Əlavə et
          </Button>
        </form>
      )}
      {createState.isError && (
        <p className="text-sm text-destructive">
          {duplicate
            ? 'Bu adda masa artıq var.'
            : 'Masanı əlavə etmək alınmadı.'}
        </p>
      )}
      {zipState.isError && (
        <p className="text-sm text-destructive">ZIP yükləmək alınmadı.</p>
      )}

      {tables.isLoading && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label="Yüklənir"
        />
      )}
      {tables.isError && (
        <p className="text-sm text-destructive">Masaları yükləmək alınmadı.</p>
      )}
      {tables.data?.length === 0 && (
        <p className="text-sm text-muted-foreground">
          Hələ masa yoxdur.
          {canCreate && ' Yuxarıdan ilk masanı əlavə edin.'}
        </p>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {tables.data?.map((table) => (
          <TableQrCard key={table.id} table={table} />
        ))}
      </div>
    </div>
  )
}
