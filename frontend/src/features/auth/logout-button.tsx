import { LogOut } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { useAppDispatch } from '@/app/hooks'
import { api } from '@/app/api'
import { useLogoutMutation } from '@/features/auth/auth-api'
import { loggedOut } from '@/features/auth/auth-slice'
import { Button } from '@/components/ui/button'

/** Clears the refresh cookie on the server and forgets the in-memory session. */
export function LogoutButton() {
  const { t } = useTranslation('auth')
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [logout, { isLoading }] = useLogoutMutation()

  async function onClick() {
    try {
      await logout().unwrap()
    } finally {
      dispatch(loggedOut())
      dispatch(api.util.resetApiState())
      navigate('/admin/login', { replace: true })
    }
  }

  return (
    <Button
      variant="outline"
      size="sm"
      onClick={() => void onClick()}
      disabled={isLoading}
      className="gap-2"
    >
      <LogOut className="size-4" aria-hidden />
      {t('logout')}
    </Button>
  )
}
