import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { LoaderCircle, ShieldX } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import type { UserRole } from '@/features/auth/auth-slice'
import { useSession } from '@/features/auth/use-session'
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

/** Where a role lands when it opens a page it may not use (e.g. the dashboard). */
const ROLE_HOME: Partial<Record<UserRole, string>> = {
  WAITER: '/admin/orders',
  CHEF: '/admin/kitchen',
}

/**
 * Guards admin routes (#17): without a session the user is sent to the login
 * page (and brought back afterwards); a signed-in user without one of the
 * allowed roles sees a "no access" card instead of the page.
 */
export function RequireAuth({
  roles,
  children,
}: {
  roles: UserRole[]
  children: ReactNode
}) {
  const { t } = useTranslation('auth')
  const { status, user } = useSession()
  const location = useLocation()

  if (status === 'unknown' || (status === 'authenticated' && !user)) {
    return (
      <main className="flex min-h-svh items-center justify-center p-6">
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label={t('guard.checkingSession')}
        />
      </main>
    )
  }

  if (status === 'anonymous' || !user) {
    return <Navigate to="/admin/login" replace state={{ from: location }} />
  }

  // Waiters and chefs have no dashboard; each has a home page in the panel.
  const home = user.role ? ROLE_HOME[user.role] : undefined
  if (home && user.role && !roles.includes(user.role)) {
    return <Navigate to={home} replace />
  }

  if (!user.role || !roles.includes(user.role)) {
    return (
      <main className="flex min-h-svh items-center justify-center p-6">
        <Card className="w-full max-w-md" role="alert">
          <CardHeader>
            <div className="flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
              <ShieldX className="size-6" aria-hidden />
            </div>
            <CardTitle>{t('guard.noAccessTitle')}</CardTitle>
            <CardDescription>{t('guard.noAccessBody')}</CardDescription>
          </CardHeader>
        </Card>
      </main>
    )
  }

  return <>{children}</>
}
