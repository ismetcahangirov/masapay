import { type FormEvent, useState } from 'react'
import { LoaderCircle, Power, UserCheck, UserPlus } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import {
  type StaffMember,
  type StaffRole,
  type StaffStatus,
  useInviteStaffMutation,
  useRestaurantStaffQuery,
  useUpdateStaffMutation,
} from '@/features/admin/staff/staff-api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
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
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

// Labels are keys of the "staff" namespace, translated at render.
const ROLE_LABELS = {
  WAITER: 'roles.WAITER',
  CHEF: 'roles.CHEF',
} as const satisfies Record<StaffRole, string>

const STATUS = {
  INVITED: { label: 'status.INVITED', variant: 'outline' },
  PENDING: { label: 'status.PENDING', variant: 'secondary' },
  ACTIVE: { label: 'status.ACTIVE', variant: 'success' },
  DEACTIVATED: { label: 'status.DEACTIVATED', variant: 'destructive' },
} as const satisfies Record<
  StaffStatus,
  {
    label: string
    variant: 'success' | 'outline' | 'secondary' | 'destructive'
  }
>

// Staff of a restaurant (#145): the manager invites a waiter or chef by the
// email of their Google account; after they sign in with Google the manager
// approves them, changes the role or switches the account off.
export function StaffPage() {
  const { t } = useTranslation('staff')
  const restaurants = useMyRestaurantsQuery()
  const [chosen, setChosen] = useState<string | null>(null)
  const restaurantId = chosen ?? restaurants.data?.[0]?.id ?? null

  const staff = useRestaurantStaffQuery(restaurantId ?? '', {
    skip: !restaurantId,
  })
  const [invite, inviteState] = useInviteStaffMutation()
  const [update, updateState] = useUpdateStaffMutation()
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<StaffRole>('WAITER')

  async function onInvite(event: FormEvent) {
    event.preventDefault()
    if (!restaurantId || !email.trim()) return
    try {
      await invite({ restaurantId, email: email.trim(), role }).unwrap()
      setEmail('')
    } catch {
      // inviteState.error is shown below.
    }
  }

  function change(
    member: StaffMember,
    patch: { role?: StaffRole; active?: boolean },
  ) {
    if (!restaurantId) return
    void update({ restaurantId, userId: member.id, ...patch })
  }

  const inviteConflict =
    inviteState.error &&
    'status' in inviteState.error &&
    inviteState.error.status === 409

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-4 sm:p-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">
          {t('page.title')}
        </h1>
        <p className="text-sm text-muted-foreground">{t('page.description')}</p>
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

      {restaurantId && (
        <form
          onSubmit={(event) => void onInvite(event)}
          className="flex flex-wrap items-end gap-3"
          aria-label={t('page.inviteForm')}
        >
          <div className="min-w-64 flex-1 space-y-1">
            <Label htmlFor="staff-email">{t('page.email')}</Label>
            <Input
              id="staff-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder={t('page.emailPlaceholder')}
              maxLength={320}
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="staff-role">{t('page.role')}</Label>
            <Select
              value={role}
              onValueChange={(value) => setRole(value as StaffRole)}
            >
              <SelectTrigger id="staff-role" className="w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {(Object.keys(ROLE_LABELS) as StaffRole[]).map((value) => (
                  <SelectItem key={value} value={value}>
                    {t(ROLE_LABELS[value])}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <Button
            type="submit"
            disabled={!email.trim() || inviteState.isLoading}
          >
            <UserPlus aria-hidden />
            {t('page.invite')}
          </Button>
        </form>
      )}
      {inviteState.isError && (
        <p className="text-sm text-destructive">
          {inviteConflict ? t('page.emailTaken') : t('page.inviteFailed')}
        </p>
      )}
      {updateState.isError && (
        <p className="text-sm text-destructive">{t('page.updateFailed')}</p>
      )}

      {staff.isLoading && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label={t('page.loading')}
        />
      )}
      {staff.isError && (
        <p className="text-sm text-destructive">{t('page.loadFailed')}</p>
      )}

      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('page.columns.member')}</TableHead>
              <TableHead>{t('page.columns.role')}</TableHead>
              <TableHead>{t('page.columns.status')}</TableHead>
              <TableHead className="sr-only">
                {t('page.columns.actions')}
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {staff.data?.map((member) => (
              <TableRow key={member.id}>
                <TableCell>
                  <p className="font-medium">{member.name ?? member.email}</p>
                  {member.name && (
                    <p className="text-xs text-muted-foreground">
                      {member.email}
                    </p>
                  )}
                </TableCell>
                <TableCell>
                  <Select
                    value={member.role}
                    onValueChange={(value) =>
                      change(member, { role: value as StaffRole })
                    }
                  >
                    <SelectTrigger
                      className="w-32"
                      aria-label={t('page.roleAria', { email: member.email })}
                    >
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {(Object.keys(ROLE_LABELS) as StaffRole[]).map(
                        (value) => (
                          <SelectItem key={value} value={value}>
                            {t(ROLE_LABELS[value])}
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>
                </TableCell>
                <TableCell>
                  <Badge variant={STATUS[member.status].variant}>
                    {t(STATUS[member.status].label)}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  {(member.status === 'PENDING' ||
                    member.status === 'DEACTIVATED') && (
                    <Button
                      size="sm"
                      variant="success"
                      onClick={() => change(member, { active: true })}
                      aria-label={t('page.approveAria', {
                        email: member.email,
                      })}
                    >
                      <UserCheck aria-hidden />
                      {member.status === 'PENDING'
                        ? t('page.approve')
                        : t('page.activate')}
                    </Button>
                  )}
                  {member.status === 'ACTIVE' && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => change(member, { active: false })}
                      aria-label={t('page.deactivateAria', {
                        email: member.email,
                      })}
                    >
                      <Power aria-hidden />
                      {t('page.deactivate')}
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {staff.data?.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={4}
                  className="py-10 text-center text-muted-foreground"
                >
                  {t('page.empty')}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
