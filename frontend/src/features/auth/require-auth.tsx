import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { LoaderCircle, ShieldX } from 'lucide-react'
import type { UserRole } from '@/features/auth/auth-slice'
import { useSession } from '@/features/auth/use-session'
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

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
  const { status, user } = useSession()
  const location = useLocation()

  if (status === 'unknown' || (status === 'authenticated' && !user)) {
    return (
      <main className="flex min-h-svh items-center justify-center p-6">
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label="Sessiya yoxlanılır"
        />
      </main>
    )
  }

  if (status === 'anonymous' || !user) {
    return <Navigate to="/admin/login" replace state={{ from: location }} />
  }

  // Waiters have no dashboard; their home in the panel is the tables page.
  if (user.role === 'WAITER' && !roles.includes('WAITER')) {
    return <Navigate to="/admin/tables" replace />
  }

  if (!user.role || !roles.includes(user.role)) {
    return (
      <main className="flex min-h-svh items-center justify-center p-6">
        <Card className="w-full max-w-md" role="alert">
          <CardHeader>
            <div className="flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
              <ShieldX className="size-6" aria-hidden />
            </div>
            <CardTitle>Giriş icazəniz yoxdur</CardTitle>
            <CardDescription>
              Bu bölmə yalnız restoran menecerləri və administratorlar üçündür.
            </CardDescription>
          </CardHeader>
        </Card>
      </main>
    )
  }

  return <>{children}</>
}
