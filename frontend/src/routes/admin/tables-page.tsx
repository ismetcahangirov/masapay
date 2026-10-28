import { type FormEvent, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { FolderDown, LoaderCircle, Plus } from 'lucide-react'
import { useAppSelector } from '@/app/hooks'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import {
  saveFile,
  useCreateTableMutation,
  useRestaurantQrZipMutation,
  useRestaurantTablesQuery,
} from '@/features/admin/tables/tables-api'
import { ServiceChargeDialog } from '@/features/admin/tables/service-charge-dialog'
import { TableQrCard } from '@/features/admin/tables/table-qr-card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

// Tables and their QR codes (#47). Managers add tables and print the codes;
// waiters see the same codes (e.g. to reprint one) but cannot add tables.
export function TablesPage() {
  const { t } = useTranslation('tables')
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
  const [chargingId, setChargingId] = useState<string | null>(null)

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
      saveFile(
        zip,
        t('page.zipFileName', {
          name: restaurantName ?? t('page.zipFallbackName'),
        }),
      )
    } catch {
      // zipState.isError is shown below.
    }
  }

  const duplicate =
    createState.error &&
    'status' in createState.error &&
    createState.error.status === 409

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-4 sm:p-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">
            {t('page.title')}
          </h1>
          <p className="text-sm text-muted-foreground">{t('page.subtitle')}</p>
        </div>
        <Button
          onClick={() => void onDownloadAll()}
          disabled={!tables.data?.length || zipState.isLoading}
        >
          <FolderDown aria-hidden />
          {zipState.isLoading ? t('page.preparing') : t('page.downloadAll')}
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
            placeholder={t('page.newTablePlaceholder')}
            maxLength={50}
            aria-label={t('page.newTableLabel')}
          />
          <Button
            type="submit"
            disabled={!label.trim() || createState.isLoading}
          >
            <Plus aria-hidden />
            {t('page.add')}
          </Button>
        </form>
      )}
      {createState.isError && (
        <p className="text-sm text-destructive">
          {duplicate ? t('page.duplicate') : t('page.createFailed')}
        </p>
      )}
      {zipState.isError && (
        <p className="text-sm text-destructive">{t('page.zipFailed')}</p>
      )}

      {tables.isLoading && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label={t('page.loading')}
        />
      )}
      {tables.isError && (
        <p className="text-sm text-destructive">{t('page.loadFailed')}</p>
      )}
      {tables.data?.length === 0 && (
        <p className="text-sm text-muted-foreground">
          {t('page.empty')}
          {canCreate && t('page.emptyHint')}
        </p>
      )}

      <div className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,14rem),1fr))] gap-4">
        {tables.data?.map((table) => (
          <TableQrCard
            key={table.id}
            table={table}
            onEditServiceCharge={
              canCreate ? () => setChargingId(table.id) : undefined
            }
          />
        ))}
      </div>

      {restaurantId && (
        <ServiceChargeDialog
          restaurantId={restaurantId}
          table={tables.data?.find((t) => t.id === chargingId) ?? null}
          onClose={() => setChargingId(null)}
        />
      )}
    </div>
  )
}
