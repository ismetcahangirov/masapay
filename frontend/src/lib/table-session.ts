// Table session token handling for the customer PWA.
//
// A masapay QR code (#47) encodes a URL of the shape
//   /r/:restaurantId/t/:tableId?token=<qrToken>
// The token is the table's random qrToken (a UUID). Knowing it is what grants
// read access to the table's live bill; printed codes do not expire. The
// backend is the source of truth (unknown token -> 404); this module only does a
// cheap format check so an obviously broken link shows an error page at once
// instead of firing a doomed request.

export type TableTokenInvalidReason = 'missing' | 'malformed'

export type TableTokenValidation =
  | { valid: true; qrToken: string }
  | { valid: false; reason: TableTokenInvalidReason }

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/**
 * Validate the table token from the QR URL query string.
 *
 * @param token The raw `token` query parameter, if any.
 */
export function validateTableToken(
  token: string | null | undefined,
): TableTokenValidation {
  if (!token) return { valid: false, reason: 'missing' }
  const trimmed = token.trim()
  if (!UUID_PATTERN.test(trimmed)) return { valid: false, reason: 'malformed' }
  return { valid: true, qrToken: trimmed.toLowerCase() }
}
