// Table session token handling for the customer PWA.
//
// A masapay QR code encodes a URL of the shape
//   /r/:restaurantSlug/t/:tableId?token=<token>
// The token is a short-lived, signed capability that grants read access to the
// live bill for a single table. The backend is the source of truth and will be
// re-validated server-side once the order API (sub-issue 4.2) lands; this
// module performs a cheap client-side pre-check so an obviously missing,
// malformed or expired token renders an error page immediately instead of
// firing a doomed request.

export type TableTokenInvalidReason = 'missing' | 'malformed' | 'expired'

export interface TableTokenClaims {
  /** Table the token was minted for. */
  tableId: string
  /** Restaurant slug the token was minted for. */
  restaurantSlug: string
  /** Expiry as a Unix timestamp in seconds. */
  exp: number
}

export type TableTokenValidation =
  | { valid: true; claims: TableTokenClaims }
  | { valid: false; reason: TableTokenInvalidReason }

function decodeBase64Url(value: string): string {
  const padded = value.replace(/-/g, '+').replace(/_/g, '/')
  const withPadding = padded.padEnd(
    padded.length + ((4 - (padded.length % 4)) % 4),
    '=',
  )
  // atob is available in every browser and in the jsdom test environment.
  return atob(withPadding)
}

function parseClaims(token: string): TableTokenClaims | null {
  try {
    const raw = JSON.parse(decodeBase64Url(token)) as Partial<TableTokenClaims>
    if (
      typeof raw.tableId === 'string' &&
      typeof raw.restaurantSlug === 'string' &&
      typeof raw.exp === 'number' &&
      Number.isFinite(raw.exp)
    ) {
      return {
        tableId: raw.tableId,
        restaurantSlug: raw.restaurantSlug,
        exp: raw.exp,
      }
    }
    return null
  } catch {
    return null
  }
}

/**
 * Validate a table-session token against the current time.
 *
 * @param token   The raw token from the QR URL query string, if any.
 * @param nowMs   Current time in milliseconds; injectable for testing.
 */
export function validateTableToken(
  token: string | null | undefined,
  nowMs: number = Date.now(),
): TableTokenValidation {
  if (!token) return { valid: false, reason: 'missing' }

  const claims = parseClaims(token)
  if (!claims) return { valid: false, reason: 'malformed' }

  if (claims.exp * 1000 <= nowMs) return { valid: false, reason: 'expired' }

  return { valid: true, claims }
}
