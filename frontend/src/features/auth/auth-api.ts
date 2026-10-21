import { api } from '@/app/api'
import type { AuthUser, UserRole } from '@/features/auth/auth-slice'

interface AuthTokensDto {
  accessToken: string
  expiresInSeconds: number
}

/** POST /api/auth/google response; `tokens` is null while the account awaits approval. */
export interface GoogleLoginResponse {
  userId: string
  email: string
  name: string | null
  enabled: boolean
  role: UserRole | null
  tokens: AuthTokensDto | null
}

export const authApi = api.injectEndpoints({
  endpoints: (build) => ({
    /** Exchanges the Google ID token (credential) for a masapay session. */
    loginWithGoogle: build.mutation<GoogleLoginResponse, string>({
      query: (idToken) => ({
        url: '/auth/google',
        method: 'POST',
        body: { idToken },
      }),
    }),
    /** LOCAL DEV ONLY: sign in as a seeded user by email (backend flag MASAPAY_DEV_LOGIN_ENABLED). */
    devLogin: build.mutation<GoogleLoginResponse, string>({
      query: (email) => ({
        url: '/auth/dev-login',
        method: 'POST',
        body: { email },
      }),
    }),
    /** New access token from the httpOnly refresh cookie (silent, no body). */
    refreshSession: build.mutation<AuthTokensDto, void>({
      query: () => ({ url: '/auth/refresh', method: 'POST' }),
    }),
    logout: build.mutation<void, void>({
      query: () => ({ url: '/auth/logout', method: 'POST' }),
    }),
    /** The signed-in user (id, email, role) from the access token. */
    me: build.query<AuthUser, void>({
      query: () => '/me',
      providesTags: ['Me'],
    }),
  }),
})

export const {
  useLoginWithGoogleMutation,
  useDevLoginMutation,
  useRefreshSessionMutation,
  useLogoutMutation,
} = authApi
