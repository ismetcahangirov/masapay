import { NavLink } from 'react-router-dom'
import {
  BookOpen,
  Building2,
  ChefHat,
  ClipboardList,
  HandCoins,
  LayoutDashboard,
  LayoutGrid,
  QrCode,
  ReceiptText,
  Settings,
  Users,
} from 'lucide-react'
import { useAppSelector } from '@/app/hooks'
import { BrandMark } from '@/components/layout/brand-mark'
import type { UserRole } from '@/features/auth/auth-slice'
import { LogoutButton } from '@/features/auth/logout-button'
import { SubscriptionBanner } from '@/features/admin/subscription-banner'
import { cn } from '@/lib/utils'

const MANAGERS: UserRole[] = ['SUPER_ADMIN', 'RESTO_MANAGER']
const STAFF: UserRole[] = [...MANAGERS, 'WAITER']
const KITCHEN: UserRole[] = [...MANAGERS, 'CHEF']
const PLATFORM: UserRole[] = ['SUPER_ADMIN']

const LINKS = [
  {
    to: '/admin/restaurants',
    label: 'Restoranlar',
    icon: Building2,
    end: false,
    roles: PLATFORM,
  },
  {
    to: '/admin',
    label: 'Dashboard',
    icon: LayoutDashboard,
    end: true,
    roles: MANAGERS,
  },
  {
    to: '/admin/live',
    label: 'Canlı masalar',
    icon: LayoutGrid,
    end: false,
    roles: STAFF,
  },
  {
    to: '/admin/tables',
    label: 'Masalar',
    icon: QrCode,
    end: false,
    roles: STAFF,
  },
  {
    to: '/admin/orders',
    label: 'Sifarişlər',
    icon: ClipboardList,
    end: false,
    roles: STAFF,
  },
  {
    to: '/admin/kitchen',
    label: 'Mətbəx',
    icon: ChefHat,
    end: false,
    roles: KITCHEN,
  },
  {
    to: '/admin/menu',
    label: 'Menyu',
    icon: BookOpen,
    end: false,
    roles: STAFF,
  },
  {
    to: '/admin/transactions',
    label: 'Əməliyyatlar',
    icon: ReceiptText,
    end: false,
    roles: MANAGERS,
  },
  {
    to: '/admin/staff',
    label: 'İşçilər',
    icon: Users,
    end: false,
    roles: MANAGERS,
  },
  {
    to: '/admin/tips',
    label: 'Bəxşişlər',
    icon: HandCoins,
    end: false,
    roles: MANAGERS,
  },
  {
    to: '/admin/settings',
    label: 'Tənzimləmələr',
    icon: Settings,
    end: false,
    roles: MANAGERS,
  },
]
/**
 * Top navigation shared by the admin pages; shows only the pages the role may
 * open, and a manager's subscription warning under it (#148).
 */
export function AdminNav() {
  const role = useAppSelector((state) => state.auth.user?.role)
  const links = LINKS.filter((link) => role && link.roles.includes(role))

  return (
    <>
      <nav className="flex flex-wrap items-center justify-between gap-4 border-b pb-4">
        <span className="flex items-center gap-2 text-xl tracking-tight">
          <BrandMark className="h-7" />
          masapay
        </span>
        <div className="flex flex-1 flex-wrap gap-1">
          {links.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-muted',
                )
              }
            >
              <Icon className="size-4" aria-hidden />
              {label}
            </NavLink>
          ))}
        </div>
        <LogoutButton />
      </nav>
      <SubscriptionBanner />
    </>
  )
}
