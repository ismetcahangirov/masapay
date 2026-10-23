import { type FormEvent, useState } from 'react'
import {
  Building2,
  CalendarClock,
  LoaderCircle,
  Plus,
  Power,
  UserPlus,
} from 'lucide-react'
import {
  type PlatformRestaurant,
  type PosType,
  type SubscriptionStatus,
  useAddManagerMutation,
  useCreateRestaurantMutation,
  usePlatformRestaurantsQuery,
  useUpdateRestaurantMutation,
} from '@/features/admin/platform/platform-api'
import {
  STATUS_LABELS,
  formatDay,
} from '@/features/admin/platform/subscription'
import { SubscriptionDialog } from '@/features/admin/platform/subscription-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

const POS_LABELS: Record<PosType, string> = {
  IIKO: 'iiko',
  MICROS: 'Micros',
  CUSTOM: 'POS-suz / digər',
}

const STATUS_VARIANTS: Record<
  SubscriptionStatus,
  'success' | 'secondary' | 'destructive' | 'outline'
> = {
  ACTIVE: 'success',
  TRIAL: 'secondary',
  EXPIRED: 'destructive',
  NONE: 'outline',
}

function isConflict(error: unknown): boolean {
  return (
    !!error &&
    typeof error === 'object' &&
    'status' in error &&
    (error as { status: unknown }).status === 409
  )
}

// The platform's restaurants (#147), for the super admin: open a restaurant
// with its first manager (who lands in it on their first Google sign-in), add
// managers, switch a restaurant off so its customers can neither order nor pay,
// and set or extend its subscription (#148).
export function RestaurantsPage() {
  const restaurants = usePlatformRestaurantsQuery()
  const [create, createState] = useCreateRestaurantMutation()
  const [update, updateState] = useUpdateRestaurantMutation()
  const [name, setName] = useState('')
  const [currency, setCurrency] = useState('AZN')
  const [posType, setPosType] = useState<PosType>('CUSTOM')
  const [managerEmail, setManagerEmail] = useState('')
  const [addingTo, setAddingTo] = useState<PlatformRestaurant | null>(null)
  const [subscribing, setSubscribing] = useState<PlatformRestaurant | null>(
    null,
  )

  const valid =
    name.trim() !== '' &&
    /^[A-Z]{3}$/.test(currency) &&
    /\S+@\S+\.\S+/.test(managerEmail.trim())

  async function onCreate(event: FormEvent) {
    event.preventDefault()
    if (!valid) return
    try {
      await create({
        name: name.trim(),
        currency,
        posType,
        managerEmail: managerEmail.trim(),
      }).unwrap()
      setName('')
      setManagerEmail('')
    } catch {
      // createState.error is shown below.
    }
  }

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-4 sm:p-6">
      <header className="space-y-1">
        <h1 className="flex items-center gap-2 text-2xl font-semibold tracking-tight">
          <Building2 className="size-6" aria-hidden />
          Restoranlar
        </h1>
        <p className="text-sm text-muted-foreground">
          Platformadakı restoranlar. Yeni restoranı ilk menecerinin Google
          emaili ilə açın: o, Google ilə daxil olan kimi restorana bağlanır.
        </p>
      </header>

      <form
        onSubmit={(event) => void onCreate(event)}
        aria-label="Yeni restoran"
        className="grid gap-3 rounded-lg border p-4 sm:grid-cols-2 lg:grid-cols-5 lg:items-end"
      >
        <div className="space-y-1 lg:col-span-2">
          <Label htmlFor="restaurant-name">Restoranın adı</Label>
          <Input
            id="restaurant-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Məsələn, Nərgiz Kafe"
            maxLength={255}
          />
        </div>
        <div className="space-y-1">
          <Label htmlFor="restaurant-currency">Valyuta</Label>
          <Input
            id="restaurant-currency"
            value={currency}
            onChange={(event) =>
              setCurrency(event.target.value.toUpperCase().slice(0, 3))
            }
            maxLength={3}
          />
        </div>
        <div className="space-y-1">
          <Label htmlFor="restaurant-pos">POS</Label>
          <Select
            value={posType}
            onValueChange={(value) => setPosType(value as PosType)}
          >
            <SelectTrigger id="restaurant-pos">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {(Object.keys(POS_LABELS) as PosType[]).map((value) => (
                <SelectItem key={value} value={value}>
                  {POS_LABELS[value]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1 sm:col-span-2 lg:col-span-4">
          <Label htmlFor="restaurant-manager">
            İlk menecerin Google emaili
          </Label>
          <Input
            id="restaurant-manager"
            type="email"
            value={managerEmail}
            onChange={(event) => setManagerEmail(event.target.value)}
            placeholder="menecer@gmail.com"
            maxLength={320}
          />
        </div>
        <Button type="submit" disabled={!valid || createState.isLoading}>
          <Plus aria-hidden />
          {createState.isLoading ? 'Yaradılır...' : 'Restoran aç'}
        </Button>
      </form>
      {createState.isError && (
        <p className="text-sm text-destructive">
          {isConflict(createState.error)
            ? 'Bu email artıq başqa restorana və ya rola aiddir.'
            : 'Restoranı açmaq alınmadı.'}
        </p>
      )}
      {updateState.isError && (
        <p className="text-sm text-destructive">Dəyişiklik alınmadı.</p>
      )}

      {restaurants.isLoading && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label="Yüklənir"
        />
      )}
      {restaurants.isError && (
        <p className="text-sm text-destructive">
          Restoranları yükləmək alınmadı.
        </p>
      )}

      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Restoran</TableHead>
              <TableHead>POS</TableHead>
              <TableHead>Menecerlər</TableHead>
              <TableHead>Abunəlik</TableHead>
              <TableHead>Vəziyyət</TableHead>
              <TableHead className="sr-only">Əməliyyat</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {restaurants.data?.map((restaurant) => (
              <TableRow key={restaurant.id}>
                <TableCell>
                  <p className="font-medium">{restaurant.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {restaurant.currency}
                  </p>
                </TableCell>
                <TableCell>{POS_LABELS[restaurant.posType]}</TableCell>
                <TableCell>
                  <ul className="space-y-1 text-sm">
                    {restaurant.managers.map((m) => (
                      <li key={m.id}>
                        {m.name ?? m.email}{' '}
                        <span className="text-xs text-muted-foreground">
                          {m.signedIn ? '' : '(hələ daxil olmayıb)'}
                        </span>
                      </li>
                    ))}
                    {restaurant.managers.length === 0 && (
                      <li className="text-muted-foreground">Menecer yoxdur</li>
                    )}
                  </ul>
                </TableCell>
                <TableCell>
                  <Badge
                    variant={STATUS_VARIANTS[restaurant.subscription.status]}
                  >
                    {STATUS_LABELS[restaurant.subscription.status]}
                  </Badge>
                  {restaurant.subscription.endsOn && (
                    <p className="mt-1 text-xs text-muted-foreground">
                      Son gün {formatDay(restaurant.subscription.endsOn)}
                    </p>
                  )}
                </TableCell>
                <TableCell>
                  <Badge
                    variant={restaurant.active ? 'success' : 'destructive'}
                  >
                    {restaurant.active ? 'Aktiv' : 'Deaktiv'}
                  </Badge>
                </TableCell>
                <TableCell className="space-x-1 whitespace-nowrap text-right">
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setSubscribing(restaurant)}
                    aria-label={`${restaurant.name} abunəlik`}
                  >
                    <CalendarClock aria-hidden />
                    Abunəlik
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setAddingTo(restaurant)}
                    aria-label={`${restaurant.name} menecer əlavə et`}
                  >
                    <UserPlus aria-hidden />
                    Menecer
                  </Button>
                  <Button
                    size="sm"
                    variant={restaurant.active ? 'outline' : 'success'}
                    onClick={() =>
                      void update({
                        id: restaurant.id,
                        active: !restaurant.active,
                      })
                    }
                    aria-label={`${restaurant.name} ${restaurant.active ? 'deaktiv et' : 'aktiv et'}`}
                  >
                    <Power aria-hidden />
                    {restaurant.active ? 'Deaktiv et' : 'Aktiv et'}
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {restaurants.data?.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="py-10 text-center text-muted-foreground"
                >
                  Hələ restoran yoxdur.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      <AddManagerDialog
        restaurant={addingTo}
        onClose={() => setAddingTo(null)}
      />
      <SubscriptionDialog
        restaurant={subscribing}
        onClose={() => setSubscribing(null)}
      />
    </div>
  )
}

function AddManagerDialog({
  restaurant,
  onClose,
}: {
  restaurant: PlatformRestaurant | null
  onClose: () => void
}) {
  const [email, setEmail] = useState('')
  const [add, addState] = useAddManagerMutation()

  function close() {
    setEmail('')
    addState.reset()
    onClose()
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!restaurant || !email.trim()) return
    try {
      await add({ id: restaurant.id, email: email.trim() }).unwrap()
      close()
    } catch {
      // addState.error is shown below.
    }
  }

  return (
    <Dialog
      open={restaurant !== null}
      onOpenChange={(open) => !open && close()}
    >
      <DialogContent className="max-w-md">
        <form onSubmit={(event) => void onSubmit(event)} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Menecer əlavə et</DialogTitle>
            <DialogDescription>
              {restaurant?.name}: menecer Google ilə daxil olan kimi bu
              restorana bağlanır.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1">
            <Label htmlFor="manager-email">Google emaili</Label>
            <Input
              id="manager-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              maxLength={320}
            />
          </div>
          {addState.isError && (
            <p className="text-sm text-destructive">
              {isConflict(addState.error)
                ? 'Bu email artıq başqa restorana və ya rola aiddir.'
                : 'Əlavə etmək alınmadı.'}
            </p>
          )}
          <DialogFooter className="gap-2">
            <Button type="button" variant="outline" onClick={close}>
              İmtina
            </Button>
            <Button
              type="submit"
              disabled={!email.trim() || addState.isLoading}
            >
              Əlavə et
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
