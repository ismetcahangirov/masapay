import { createSlice, type PayloadAction } from '@reduxjs/toolkit'

// Admin session (#17). The access token is kept in memory only (this slice),
// never in localStorage, so a script injected into the page cannot read a
// stored login. The refresh token is an httpOnly cookie the browser sends to
// /api/auth by itself; a page reload restores the session through it.

export type UserRole = 'SUPER_ADMIN' | 'RESTO_MANAGER' | 'WAITER'

export interface AuthUser {
  id: string
  email: string
  role: UserRole | null
}

export type SessionStatus =
  /** Not checked yet: a silent refresh may still restore the session. */
  'unknown' | 'authenticated' | 'anonymous'

export interface AuthState {
  accessToken: string | null
  user: AuthUser | null
  status: SessionStatus
}

const initialState: AuthState = {
  accessToken: null,
  user: null,
  status: 'unknown',
}

export const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    /** A new access token (login or silent refresh); the user is kept unless given. */
    tokenReceived(
      state,
      action: PayloadAction<{ accessToken: string; user?: AuthUser }>,
    ) {
      state.accessToken = action.payload.accessToken
      if (action.payload.user) state.user = action.payload.user
      state.status = 'authenticated'
    },
    userLoaded(state, action: PayloadAction<AuthUser>) {
      state.user = action.payload
    },
    loggedOut(state) {
      state.accessToken = null
      state.user = null
      state.status = 'anonymous'
    },
  },
})

export const { tokenReceived, userLoaded, loggedOut } = authSlice.actions
