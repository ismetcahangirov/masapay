import type { FetchBaseQueryError } from '@reduxjs/toolkit/query'
import type { SerializedError } from '@reduxjs/toolkit'
import { problemReason, takesNoOnlinePayments } from '@/lib/api-errors'
import i18n from '@/i18n'

/** Turns an API error into a short message for the customer. */
export function paymentErrorMessage(
  error: FetchBaseQueryError | SerializedError | undefined,
): string | null {
  if (!error) return null
  if ('status' in error && error.status === 'FETCH_ERROR') {
    return i18n.t('payment:errors.offline')
  }
  // Payriff did not open the payment page (down, or an error of its own).
  if (problemReason(error) === 'PAYRIFF_UNAVAILABLE') {
    return i18n.t('payment:errors.payriffUnavailable')
  }
  if (takesNoOnlinePayments(error)) {
    return i18n.t('payment:errors.noOnlinePayments')
  }
  if ('status' in error && error.status === 409) {
    return i18n.t('payment:errors.conflict')
  }
  if ('status' in error && error.status === 400) {
    return i18n.t('payment:errors.badChoice')
  }
  return i18n.t('payment:errors.generic')
}
