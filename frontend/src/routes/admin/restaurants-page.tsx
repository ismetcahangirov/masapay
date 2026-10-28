import { type FormEvent, useState } from 'react'
import {
  Building2,
  CalendarClock,
  LoaderCircle,
  Plus,
  Power,
  UserPlus,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
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

// Keys of the "platform" namespace, translated at render.
const POS_LABELS = {
  IIKO: 'restaurants.pos.IIKO',
  MICROS: 'restaurants.pos.MICROS',
  CUSTOM: 'restaurants.pos.CUSTOM',
} as const satisfies Record<PosType, string>

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
  const { t } = useTranslation('platform')
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
          {t('restaurants.title')}
        </h1>
        <p className="text-sm text-muted-foreground">
          {t('restaurants.description')}
        </p>
      </header>

      <form
        onSubmit={(event) => void onCreate(event)}
        aria-label={t('restaurants.newRestaurant')}
        className="grid gap-3 rounded-lg border p-4 sm:grid-cols-2 lg:grid-cols-5 lg:items-end"
      >
        <div className="space-y-1 lg:col-span-2">
          <Label htmlFor="restaurant-name">{t('restaurants.name')}</Label>
          <Input
            id="restaurant-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder={t('restaurants.namePlaceholder')}
            maxLength={255}
          />
        </div>
        <div className="space-y-1">
          <Label htmlFor="restaurant-currency">
            {t('restaurants.currency')}
          </Label>
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
          <Label htmlFor="restaurant-pos">{t('restaurants.posLabel')}</Label>
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
                  {t(POS_LABELS[value])}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1 sm:col-span-2 lg:col-span-4">
          <Label htmlFor="restaurant-manager">
            {t('restaurants.managerEmail')}
          </Label>
          <Input
            id="restaurant-manager"
            type="email"
            value={managerEmail}
            onChange={(event) => setManagerEmail(event.target.value)}
            placeholder={t('restaurants.managerEmailPlaceholder')}
            maxLength={320}
          />
        </div>
        <Button type="submit" disabled={!valid || createState.isLoading}>
          <Plus aria-hidden />
          {createState.isLoading
            ? t('restaurants.creating')
            : t('restaurants.create')}
        </Button>
      </form>
      {createState.isError && (
        <p className="text-sm text-destructive">
          {isConflict(createState.error)
            ? t('restaurants.emailTaken')
            : t('restaurants.createFailed')}
        </p>
      )}
      {updateState.isError && (
        <p className="text-sm text-destructive">
          {t('restaurants.updateFailed')}
        </p>
      )}

      {restaurants.isLoading && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label={t('restaurants.loading')}
        />
      )}
      {restaurants.isError && (
        <p className="text-sm text-destructive">
          {t('restaurants.loadFailed')}
        </p>
      )}

      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('restaurants.columns.restaurant')}</TableHead>
              <TableHead>{t('restaurants.columns.pos')}</TableHead>
              <TableHead>{t('restaurants.columns.managers')}</TableHead>
              <TableHead>{t('restaurants.columns.subscription')}</TableHead>
              <TableHead>{t('restaurants.columns.status')}</TableHead>
              <TableHead className="sr-only">
                {t('restaurants.columns.actions')}
              </TableHead>
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
                <TableCell>{t(POS_LABELS[restaurant.posType])}</TableCell>
                <TableCell>
                  <ul className="space-y-1 text-sm">
                    {restaurant.managers.map((m) => (
                      <li key={m.id}>
                        {m.name ?? m.email}{' '}
                        <span className="text-xs text-muted-foreground">
                          {m.signedIn ? '' : t('restaurants.notSignedIn')}
                        </span>
                      </li>
                    ))}
                    {restaurant.managers.length === 0 && (
                      <li className="text-muted-foreground">
                        {t('restaurants.noManagers')}
                      </li>
                    )}
                  </ul>
                </TableCell>
                <TableCell>
                  <Badge
                    variant={STATUS_VARIANTS[restaurant.subscription.status]}
                  >
                    {t(STATUS_LABELS[restaurant.subscription.status])}
                  </Badge>
                  {restaurant.subscription.endsOn && (
                    <p className="mt-1 text-xs text-muted-foreground">
                      {t('restaurants.endsOn', {
                        day: formatDay(restaurant.subscription.endsOn),
                      })}
                    </p>
                  )}
                </TableCell>
                <TableCell>
                  <Badge
                    variant={restaurant.active ? 'success' : 'destructive'}
                  >
                    {restaurant.active
                      ? t('restaurants.active')
                      : t('restaurants.inactive')}
                  </Badge>
                </TableCell>
                <TableCell className="space-x-1 whitespace-nowrap text-right">
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setSubscribing(restaurant)}
                    aria-label={t('restaurants.subscriptionAria', {
                      name: restaurant.name,
                    })}
                  >
                    <CalendarClock aria-hidden />
                    {t('restaurants.subscription')}
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setAddingTo(restaurant)}
                    aria-label={t('restaurants.addManagerAria', {
                      name: restaurant.name,
                    })}
                  >
                    <UserPlus aria-hidden />
                    {t('restaurants.manager')}
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
                    aria-label={
                      restaurant.active
                        ? t('restaurants.deactivateAria', {
                            name: restaurant.name,
                          })
                        : t('restaurants.activateAria', {
                            name: restaurant.name,
                          })
                    }
                  >
                    <Power aria-hidden />
                    {restaurant.active
                      ? t('restaurants.deactivate')
                      : t('restaurants.activate')}
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
                  {t('restaurants.empty')}
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
  const { t } = useTranslation('platform')
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
            <DialogTitle>{t('addManager.title')}</DialogTitle>
            <DialogDescription>
              {t('addManager.description', { name: restaurant?.name })}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1">
            <Label htmlFor="manager-email">{t('addManager.email')}</Label>
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
                ? t('addManager.emailTaken')
                : t('addManager.addFailed')}
            </p>
          )}
          <DialogFooter className="gap-2">
            <Button type="button" variant="outline" onClick={close}>
              {t('addManager.cancel')}
            </Button>
            <Button
              type="submit"
              disabled={!email.trim() || addState.isLoading}
            >
              {t('addManager.add')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
