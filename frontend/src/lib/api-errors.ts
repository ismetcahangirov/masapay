import type { SerializedError } from '@reduxjs/toolkit'
import type { FetchBaseQueryError } from '@reduxjs/toolkit/query'

type ApiError = FetchBaseQueryError | SerializedError | undefined

/**
 * The short reason the backend put in an RFC 7807 problem detail ("detail"),
 * e.g. RESTAURANT_CLOSED; null when there is none.
 */
export function problemReason(error: ApiError): string | null {
  if (!error || !('status' in error)) return null
  const data = error.data as { detail?: unknown } | undefined
  return typeof data?.detail === 'string' ? data.detail : null
}

/** The restaurant was switched off by the platform (#147). */
export function isRestaurantClosed(error: ApiError): boolean {
  return problemReason(error) === 'RESTAURANT_CLOSED'
}
