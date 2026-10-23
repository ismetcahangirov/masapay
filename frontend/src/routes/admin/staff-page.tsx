import { type FormEvent, useState } from 'react'
import { LoaderCircle, Power, UserCheck, UserPlus } from 'lucide-react'
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

const ROLE_LABELS: Record<StaffRole, string> = {
  WAITER: 'Ofisiant',
  CHEF: 'Aşpaz',
}

const STATUS: Record<
  StaffStatus,
  {
    label: string
    variant: 'success' | 'outline' | 'secondary' | 'destructive'
  }
> = {
  INVITED: { label: 'Dəvət olunub, hələ daxil olmayıb', variant: 'outline' },
  PENDING: { label: 'Təsdiq gözləyir', variant: 'secondary' },
  ACTIVE: { label: 'Aktiv', variant: 'success' },
  DEACTIVATED: { label: 'Deaktiv', variant: 'destructive' },
}

// Staff of a restaurant (#145): the manager invites a waiter or chef by the
// email of their Google account; after they sign in with Google the manager
// approves them, changes the role or switches the account off.
export function StaffPage() {
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
        <h1 className="text-2xl font-semibold tracking-tight">İşçilər</h1>
        <p className="text-sm text-muted-foreground">
          Ofisiant və aşpazları Google hesablarının emaili ilə dəvət edin. Onlar
          Google ilə daxil olandan sonra burada təsdiqləyin.
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

      {restaurantId && (
        <form
          onSubmit={(event) => void onInvite(event)}
          className="flex flex-wrap items-end gap-3"
          aria-label="İşçi dəvət et"
        >
          <div className="min-w-64 flex-1 space-y-1">
            <Label htmlFor="staff-email">Google emaili</Label>
            <Input
              id="staff-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="ad.soyad@gmail.com"
              maxLength={320}
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="staff-role">Rol</Label>
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
                    {ROLE_LABELS[value]}
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
            Dəvət et
          </Button>
        </form>
      )}
      {inviteState.isError && (
        <p className="text-sm text-destructive">
          {inviteConflict
            ? 'Bu email artıq bir işçiyə və ya başqa restorana aiddir.'
            : 'Dəvət etmək alınmadı. Emaili yoxlayın.'}
        </p>
      )}
      {updateState.isError && (
        <p className="text-sm text-destructive">Dəyişiklik alınmadı.</p>
      )}

      {staff.isLoading && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label="Yüklənir"
        />
      )}
      {staff.isError && (
        <p className="text-sm text-destructive">İşçiləri yükləmək alınmadı.</p>
      )}

      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>İşçi</TableHead>
              <TableHead>Rol</TableHead>
              <TableHead>Vəziyyət</TableHead>
              <TableHead className="sr-only">Əməliyyat</TableHead>
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
                      aria-label={`${member.email} rolu`}
                    >
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {(Object.keys(ROLE_LABELS) as StaffRole[]).map(
                        (value) => (
                          <SelectItem key={value} value={value}>
                            {ROLE_LABELS[value]}
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>
                </TableCell>
                <TableCell>
                  <Badge variant={STATUS[member.status].variant}>
                    {STATUS[member.status].label}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  {(member.status === 'PENDING' ||
                    member.status === 'DEACTIVATED') && (
                    <Button
                      size="sm"
                      variant="success"
                      onClick={() => change(member, { active: true })}
                      aria-label={`${member.email} təsdiqlə`}
                    >
                      <UserCheck aria-hidden />
                      {member.status === 'PENDING' ? 'Təsdiqlə' : 'Aktiv et'}
                    </Button>
                  )}
                  {member.status === 'ACTIVE' && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => change(member, { active: false })}
                      aria-label={`${member.email} deaktiv et`}
                    >
                      <Power aria-hidden />
                      Deaktiv et
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
                  Hələ işçi yoxdur. Yuxarıdan ilk ofisiant və ya aşpazı dəvət
                  edin.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
