import { describe, expect, it } from 'vitest'
import { paymentErrorMessage } from '@/features/payment/payment-errors'
import {
  isRestaurantClosed,
  problemReason,
  takesNoOnlinePayments,
} from './api-errors'

const closed = {
  status: 409,
  data: { type: 'about:blank', status: 409, detail: 'RESTAURANT_CLOSED' },
} as const

describe('API problem details (#147)', () => {
  it('reads the reason the backend sent', () => {
    expect(problemReason(closed)).toBe('RESTAURANT_CLOSED')
    expect(isRestaurantClosed(closed)).toBe(true)
    expect(
      isRestaurantClosed({
        status: 409,
        data: { detail: 'Order has nothing left to pay' },
      }),
    ).toBe(false)
    expect(
      problemReason({ status: 'FETCH_ERROR', error: 'offline' }),
    ).toBeNull()
    expect(problemReason(undefined)).toBeNull()
  })

  it('tells the customer the restaurant takes no online payments', () => {
    expect(paymentErrorMessage(closed)).toMatch(/onlayn ödəniş qəbul etmir/)
    // Other conflicts keep the old message.
    expect(paymentErrorMessage({ status: 409, data: {} })).toMatch(
      /Hesab bu arada dəyişdi/,
    )
  })

  it('treats an ended subscription like a closed restaurant for payments (#148)', () => {
    const expired = { status: 409, data: { detail: 'SUBSCRIPTION_EXPIRED' } }
    expect(takesNoOnlinePayments(expired)).toBe(true)
    expect(takesNoOnlinePayments(closed)).toBe(true)
    // Ordering stays open, so it is not "closed".
    expect(isRestaurantClosed(expired)).toBe(false)
    expect(paymentErrorMessage(expired)).toMatch(/onlayn ödəniş qəbul etmir/)
  })
})
