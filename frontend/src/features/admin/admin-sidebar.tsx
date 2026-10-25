import { NavLink, useLocation } from 'react-router-dom'
import {
  BookOpen,
  Building2,
  ChartColumn,
  ChefHat,
  ClipboardList,
  HandCoins,
  LayoutDashboard,
  type LucideIcon,
  MonitorDot,
  QrCode,
  ReceiptText,
  Settings,
  Users,
} from 'lucide-react'
import { useAppSelector } from '@/app/hooks'
import { BrandMark } from '@/components/layout/brand-mark'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
} from '@/components/ui/sidebar'
import type { UserRole } from '@/features/auth/auth-slice'
import { LogoutButton } from '@/features/auth/logout-button'

const MANAGERS: UserRole[] = ['SUPER_ADMIN', 'RESTO_MANAGER']
const STAFF: UserRole[] = [...MANAGERS, 'WAITER']
const KITCHEN: UserRole[] = [...MANAGERS, 'CHEF']
const PLATFORM: UserRole[] = ['SUPER_ADMIN']

export interface AdminLink {
  to: string
  label: string
  icon: LucideIcon
  /** Active only on this exact path (the dashboard is the parent of all). */
  end: boolean
  roles: UserRole[]
}

/** The platform's own pages, for the super admin (#147, #149). */
const PLATFORM_LINKS: AdminLink[] = [
  {
    to: '/admin/restaurants',
    label: 'Restoranlar',
    icon: Building2,
    end: false,
    roles: PLATFORM,
  },
  {
    to: '/admin/analytics',
    label: 'Analitika',
    icon: ChartColumn,
    end: false,
    roles: PLATFORM,
  },
]

/** A restaurant's pages; each role sees only the ones it may open. */
const RESTAURANT_LINKS: AdminLink[] = [
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
    icon: MonitorDot,
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
    to: '/admin/my-tips',
    label: 'Bəxşişlərim',
    icon: HandCoins,
    end: false,
    roles: ['WAITER'],
  },
  {
    to: '/admin/settings',
    label: 'Tənzimləmələr',
    icon: Settings,
    end: false,
    roles: MANAGERS,
  },
]

function isActive(pathname: string, link: AdminLink): boolean {
  return link.end
    ? pathname === link.to
    : pathname === link.to || pathname.startsWith(`${link.to}/`)
}

function LinkGroup({
  label,
  links,
  pathname,
}: {
  label: string
  links: AdminLink[]
  pathname: string
}) {
  if (links.length === 0) return null
  return (
    <SidebarGroup>
      <SidebarGroupLabel>{label}</SidebarGroupLabel>
      <SidebarGroupContent>
        <SidebarMenu>
          {links.map((link) => (
            <SidebarMenuItem key={link.to}>
              <SidebarMenuButton
                asChild
                isActive={isActive(pathname, link)}
                tooltip={link.label}
              >
                <NavLink to={link.to} end={link.end}>
                  <link.icon aria-hidden />
                  <span>{link.label}</span>
                </NavLink>
              </SidebarMenuButton>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  )
}

/**
 * The admin panel's navigation (#184): a shadcn sidebar that collapses to
 * icons (with the page name as a tooltip) and opens as a sheet on phones.
 * Links are filtered by role, as the old header did.
 */
export function AdminSidebar() {
  const user = useAppSelector((state) => state.auth.user)
  const { pathname } = useLocation()
  const role = user?.role
  const allowed = (links: AdminLink[]) =>
    links.filter((link) => role && link.roles.includes(role))

  return (
    <Sidebar
      collapsible="icon"
      role="navigation"
      aria-label="Admin naviqasiyası"
    >
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            {/* Collapsed, the logo gets the same padding as the icons, so it sits in their column. */}
            <SidebarMenuButton
              size="lg"
              asChild
              tooltip="masapay"
              className="group-data-[collapsible=icon]:!p-2"
            >
              <NavLink
                to={role === 'SUPER_ADMIN' ? '/admin/restaurants' : '/admin'}
              >
                <BrandMark className="size-6 shrink-0" />
                <span className="text-lg tracking-tight group-data-[collapsible=icon]:hidden">
                  masapay
                </span>
              </NavLink>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <LinkGroup
          label="Platforma"
          links={allowed(PLATFORM_LINKS)}
          pathname={pathname}
        />
        <LinkGroup
          label="Restoran"
          links={allowed(RESTAURANT_LINKS)}
          pathname={pathname}
        />
      </SidebarContent>
      <SidebarFooter className="gap-2 group-data-[collapsible=icon]:hidden">
        {user && (
          <p className="truncate px-2 text-xs text-muted-foreground">
            {user.email}
          </p>
        )}
        <LogoutButton />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
