import type { SerializedError } from '@reduxjs/toolkit'
import type { FetchBaseQueryError } from '@reduxjs/toolkit/query'

/** 409 from the menu API: a duplicate name, or a category that still has dishes. */
export function isConflict(
  error: FetchBaseQueryError | SerializedError | undefined,
): boolean {
  return !!error && 'status' in error && error.status === 409
}
