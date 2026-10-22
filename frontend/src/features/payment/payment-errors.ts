import type { FetchBaseQueryError } from '@reduxjs/toolkit/query'
import type { SerializedError } from '@reduxjs/toolkit'
import { isRestaurantClosed } from '@/lib/api-errors'

/** Turns an API error into a short message for the customer. */
export function paymentErrorMessage(
  error: FetchBaseQueryError | SerializedError | undefined,
): string | null {
  if (!error) return null
  if ('status' in error && error.status === 'FETCH_ERROR') {
    return 'İnternet bağlantısı yoxdur. Bağlantını yoxlayıb yenidən cəhd edin, pul çəkilməyib.'
  }
  if (isRestaurantClosed(error)) {
    return 'Restoran hazırda onlayn ödəniş qəbul etmir. Zəhmət olmasa ofisianta müraciət edin.'
  }
  if ('status' in error && error.status === 409) {
    return 'Hesab bu arada dəyişdi (kimsə artıq ödəmiş ola bilər). Məbləği yoxlayıb yenidən cəhd edin.'
  }
  if ('status' in error && error.status === 400) {
    return 'Seçim yanlışdır. Zəhmət olmasa yenidən seçin.'
  }
  return 'Ödəniş alınmadı. İnternet bağlantınızı yoxlayıb yenidən cəhd edin.'
}
