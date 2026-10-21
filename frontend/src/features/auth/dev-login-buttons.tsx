import { useState } from 'react'
import { FlaskConical } from 'lucide-react'
import {
  type GoogleLoginResponse,
  useDevLoginMutation,
} from '@/features/auth/auth-api'
import { Button } from '@/components/ui/button'

// LOCAL DEVELOPMENT ONLY: sign in as a seeded user without Google. Rendered
// only in `npm run dev` (import.meta.env.DEV), and the backend endpoint only
// exists when started with MASAPAY_DEV_LOGIN_ENABLED=true.
const TEST_USERS = [
  { email: 'admin@masapay.local', label: 'Test admini (SUPER_ADMIN)' },
  { email: 'manager@dolma.local', label: 'Test meneceri (Dolma Restoran)' },
]

export function DevLoginButtons({
  onResponse,
}: {
  onResponse: (response: GoogleLoginResponse) => void
}) {
  const [devLogin, { isLoading }] = useDevLoginMutation()
  const [failed, setFailed] = useState(false)

  async function signIn(email: string) {
    setFailed(false)
    try {
      onResponse(await devLogin(email).unwrap())
    } catch {
      setFailed(true)
    }
  }

  return (
    <div className="w-full space-y-2 border-t pt-4">
      <p className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
        <FlaskConical className="size-3.5" aria-hidden />
        Lokal test girişi (Google-suz)
      </p>
      {TEST_USERS.map((user) => (
        <Button
          key={user.email}
          variant="outline"
          className="w-full"
          disabled={isLoading}
          onClick={() => void signIn(user.email)}
        >
          {user.label}
        </Button>
      ))}
      {failed && (
        <p className="text-center text-xs text-destructive">
          Test girişi alınmadı. Backend MASAPAY_DEV_LOGIN_ENABLED=true ilə işə
          salınmalıdır.
        </p>
      )}
    </div>
  )
}
