import { useState } from 'react'
import { GoogleLogin } from '@react-oauth/google'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Clock, CircleAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useAppDispatch } from '@/app/hooks'
import { BrandMark } from '@/components/layout/brand-mark'
import { LanguageSwitcher } from '@/components/layout/language-switcher'
import {
  type GoogleLoginResponse,
  useLoginWithGoogleMutation,
} from '@/features/auth/auth-api'
import { DevLoginButtons } from '@/features/auth/dev-login-buttons'
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
  const { t } = useTranslation('auth')
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
      finish(await login(credential).unwrap())
    } catch {
      setProblem('failed')
    }
  }

  function finish(response: GoogleLoginResponse) {
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
  }

  return (
    <main className="relative flex min-h-svh items-center justify-center bg-muted/40 p-4 sm:p-6">
      <LanguageSwitcher className="absolute right-4 top-4 bg-background sm:right-6 sm:top-6" />
      <Card className="w-full max-w-sm">
        <CardHeader className="items-center text-center">
          <BrandMark className="mb-2 h-12" />
          <CardTitle>{t('login.title')}</CardTitle>
          <CardDescription>{t('login.subtitle')}</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col items-center gap-4">
          {googleClientId ? (
            <GoogleLogin
              onSuccess={(result) => void onCredential(result.credential)}
              onError={() => setProblem('failed')}
            />
          ) : (
            <p className="text-center text-sm text-muted-foreground">
              {t('login.notConfigured')}
            </p>
          )}

          {isLoading && (
            <p className="text-sm text-muted-foreground">
              {t('login.checking')}
            </p>
          )}

          {problem === 'pending' && (
            <p className="flex items-start gap-2 text-sm text-muted-foreground">
              <Clock className="mt-0.5 size-4 shrink-0" aria-hidden />
              {t('login.pending')}
            </p>
          )}
          {problem === 'failed' && (
            <p className="flex items-start gap-2 text-sm text-destructive">
              <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
              {t('login.failed')}
            </p>
          )}
          {import.meta.env.DEV && <DevLoginButtons onResponse={finish} />}
        </CardContent>
      </Card>
    </main>
  )
}
