import { NavLink } from 'react-router-dom'
import { LayoutDashboard, Settings } from 'lucide-react'
import { LogoutButton } from '@/features/auth/logout-button'
import { cn } from '@/lib/utils'

const LINKS = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/admin/settings', label: 'Tənzimləmələr', icon: Settings, end: false },
]

/** Top navigation shared by the admin pages. */
export function AdminNav() {
  return (
    <nav className="flex items-center justify-between gap-4 border-b pb-4">
      <div className="flex gap-1">
        {LINKS.map(({ to, label, icon: Icon, end }) => (
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
  )
}
