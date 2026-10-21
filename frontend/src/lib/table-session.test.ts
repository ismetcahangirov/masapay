import { describe, expect, it } from 'vitest'
import { validateTableToken } from '@/lib/table-session'

describe('validateTableToken (QR link)', () => {
  it('accepts the table qrToken from the QR code', () => {
    expect(validateTableToken('11111111-0000-0000-0000-00000000000A')).toEqual({
      valid: true,
      qrToken: '11111111-0000-0000-0000-00000000000a',
    })
  })

  it('rejects a missing or broken token before calling the backend', () => {
    expect(validateTableToken(null)).toEqual({
      valid: false,
      reason: 'missing',
    })
    expect(validateTableToken('not-a-token')).toEqual({
      valid: false,
      reason: 'malformed',
    })
  })
})
