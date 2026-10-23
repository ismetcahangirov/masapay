import { useState } from 'react'
import { LoaderCircle } from 'lucide-react'
import {
  useIntegrationSettingsQuery,
  useMyRestaurantsQuery,
} from '@/features/admin/settings/settings-api'
import { SettingsForm } from '@/features/admin/settings/settings-form'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

// Admin settings (#46): service charge, POS provider and Payriff keys of a
// restaurant. A manager sees their own restaurant; a super admin picks one.
export function SettingsPage() {
  const restaurants = useMyRestaurantsQuery()
  const [chosen, setChosen] = useState<string | null>(null)
  const restaurantId = chosen ?? restaurants.data?.[0]?.id ?? null

  const settings = useIntegrationSettingsQuery(restaurantId ?? '', {
    skip: !restaurantId,
  })

  return (
    <div className="mx-auto w-full max-w-3xl space-y-6 p-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">Tənzimləmələr</h1>
        <p className="text-sm text-muted-foreground">
          Restoranın xidmət haqqı, POS və ödəniş ayarları
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

      {restaurants.data?.length === 0 && (
        <p className="text-sm text-muted-foreground">
          Hesabınıza hələ restoran təyin olunmayıb.
        </p>
      )}

      {(restaurants.isLoading || settings.isFetching) && !settings.data && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label="Yüklənir"
        />
      )}
      {(restaurants.isError || settings.isError) && (
        <p className="text-sm text-destructive">
          Tənzimləmələri yükləmək alınmadı.
        </p>
      )}
      {settings.data && <SettingsForm settings={settings.data} />}
    </div>
  )
}
