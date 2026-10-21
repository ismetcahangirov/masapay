import { useState } from 'react'
import { GoogleLogin } from '@react-oauth/google'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Clock, CircleAlert } from 'lucide-react'
import { useAppDispatch } from '@/app/hooks'
import { useLoginWithGoogleMutation } from '@/features/auth/auth-api'
import { tokenReceived } from '@/features/auth/auth-slice'
import { useSession } from '@/features/auth/use-session'
import { googleClientId } from '@/features/auth/google'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

type LoginProblem = 'pending' | 'failed' | null

// Admin sign-in with Google (#17). Google returns an ID token (credential); the
// backend verifies it and answers with an access token (kept in memory) and
// the refresh cookie. New accounts wait for a SUPER_ADMIN to enable them.
export function LoginPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const { status } = useSession()
  const [login, { isLoading }] = useLoginWithGoogleMutation()
  const [problem, setProblem] = useState<LoginProblem>(null)

  const from =
    (location.state as { from?: { pathname: string } } | null)?.from
      ?.pathname ?? '/admin'

  if (status === 'authenticated') {
    return <Navigate to={from} replace />
  }

  async function onCredential(credential: string | undefined) {
    if (!credential) {
      setProblem('failed')
      return
    }
    try {
      const response = await login(credential).unwrap()
      if (!response.enabled || !response.tokens) {
        setProblem('pending')
        return
      }
      dispatch(
        tokenReceived({
          accessToken: response.tokens.accessToken,
          user: {
            id: response.userId,
            email: response.email,
            role: response.role,
          },
        }),
      )
      navigate(from, { replace: true })
    } catch {
      setProblem('failed')
    }
  }

  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/40 p-6">
      <Card className="w-full max-w-sm">
        <CardHeader className="text-center">
          <CardTitle>masapay admin</CardTitle>
          <CardDescription>Google hesabınızla daxil olun</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col items-center gap-4">
          {googleClientId ? (
            <GoogleLogin
              onSuccess={(result) => void onCredential(result.credential)}
              onError={() => setProblem('failed')}
            />
          ) : (
            <p className="text-center text-sm text-muted-foreground">
              Google girişi konfiqurasiya olunmayıb: frontend/.env.local
              faylında VITE_GOOGLE_CLIENT_ID təyin edin.
            </p>
          )}

          {isLoading && (
            <p className="text-sm text-muted-foreground">Yoxlanılır...</p>
          )}

          {problem === 'pending' && (
            <p className="flex items-start gap-2 text-sm text-muted-foreground">
              <Clock className="mt-0.5 size-4 shrink-0" aria-hidden />
              Hesabınız yaradıldı və administratorun təsdiqini gözləyir.
            </p>
          )}
          {problem === 'failed' && (
            <p className="flex items-start gap-2 text-sm text-destructive">
              <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
              Giriş alınmadı. Yenidən cəhd edin.
            </p>
          )}
        </CardContent>
      </Card>
    </main>
  )
}
