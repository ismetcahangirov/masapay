import { useEffect } from 'react'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { authApi } from '@/features/auth/auth-api'
import {
  loggedOut,
  tokenReceived,
  userLoaded,
} from '@/features/auth/auth-slice'

// Restores the admin session after a page reload (#17): the in-memory access
// token is gone, but the httpOnly refresh cookie survives, so one silent
// refresh plus /me brings the user back without the login page.
let restoring: Promise<void> | null = null

export function useSession() {
  const dispatch = useAppDispatch()
  const auth = useAppSelector((state) => state.auth)

  useEffect(() => {
    if (auth.status !== 'unknown' || restoring) return
    restoring = (async () => {
      try {
        const tokens = await dispatch(
          authApi.endpoints.refreshSession.initiate(),
        ).unwrap()
        dispatch(tokenReceived({ accessToken: tokens.accessToken }))
        const me = await dispatch(
          authApi.endpoints.me.initiate(undefined, { forceRefetch: true }),
        ).unwrap()
        dispatch(userLoaded(me))
      } catch {
        dispatch(loggedOut())
      }
    })().finally(() => {
      restoring = null
    })
  }, [auth.status, dispatch])

  return auth
}
