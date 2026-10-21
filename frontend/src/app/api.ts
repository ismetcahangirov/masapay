import {
  createApi,
  fetchBaseQuery,
  type BaseQueryFn,
  type FetchArgs,
  type FetchBaseQueryError,
} from '@reduxjs/toolkit/query/react'
import { loggedOut, tokenReceived } from '@/features/auth/auth-slice'
import type { AuthState } from '@/features/auth/auth-slice'

// Single RTK Query API for the whole app. Feature slices add their endpoints
// with `api.injectEndpoints` so each feature owns its own requests.

const rawBaseQuery = fetchBaseQuery({
  baseUrl: '/api',
  // Sends the httpOnly refresh cookie to /api/auth (same origin via the proxy).
  credentials: 'same-origin',
  prepareHeaders: (headers, { getState }) => {
    const token = (getState() as { auth: AuthState }).auth.accessToken
    if (token && !headers.has('Authorization')) {
      headers.set('Authorization', `Bearer ${token}`)
    }
    return headers
  },
})

interface RefreshResponse {
  accessToken: string
}

// One refresh at a time: if five requests get 401 together, they all wait for
// the same refresh call instead of each rotating the cookie.
let refreshing: Promise<string | null> | null = null

function isAuthCall(args: string | FetchArgs): boolean {
  const url = typeof args === 'string' ? args : args.url
  return url.startsWith('/auth/')
}

/**
 * Silent token refresh (#17): when an API call returns 401 because the short
 * access token expired, ask /auth/refresh for a new one (the refresh cookie
 * goes along automatically) and retry the call once. The user never sees it.
 */
const baseQueryWithReauth: BaseQueryFn<
  string | FetchArgs,
  unknown,
  FetchBaseQueryError
> = async (args, apiContext, extraOptions) => {
  let result = await rawBaseQuery(args, apiContext, extraOptions)
  if (result.error?.status !== 401 || isAuthCall(args)) return result

  if (!refreshing) {
    refreshing = (async () => {
      const refreshed = await rawBaseQuery(
        { url: '/auth/refresh', method: 'POST' },
        apiContext,
        extraOptions,
      )
      if (refreshed.data) {
        const { accessToken } = refreshed.data as RefreshResponse
        apiContext.dispatch(tokenReceived({ accessToken }))
        return accessToken
      }
      apiContext.dispatch(loggedOut())
      return null
    })().finally(() => {
      refreshing = null
    })
  }

  const accessToken = await refreshing
  if (accessToken) {
    result = await rawBaseQuery(args, apiContext, extraOptions)
  }
  return result
}

export const api = createApi({
  reducerPath: 'api',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['TableOrder', 'Me', 'Settings'],
  endpoints: () => ({}),
})
