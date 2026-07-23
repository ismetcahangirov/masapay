import type { PaymentStatus } from '@/features/payment/types'

// The Google "write a review" deep link for a place. Opening it takes the guest
// straight to the review composer for that business.
const GOOGLE_WRITE_REVIEW_BASE =
  'https://search.google.com/local/writereview?placeid='

/** Build the Google review deep link for a restaurant's Place ID. */
export function buildGoogleReviewUrl(placeId: string): string {
  return `${GOOGLE_WRITE_REVIEW_BASE}${encodeURIComponent(placeId)}`
}

/**
 * The review prompt appears only after a successful payment.
 *
 * Acceptance criterion (6.3): the modal is shown only when the payment status
 * is APPROVED.
 */
export function shouldShowReviewModal(status: PaymentStatus): boolean {
  return status === 'APPROVED'
}
