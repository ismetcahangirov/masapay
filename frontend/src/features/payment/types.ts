// Payment status shared across the payment flow and post-payment surfaces.
//
// Mirrors the Payriff transaction states the backend reports. The frontend
// payment flow (sub-issue 7.5) sets these from the Payriff result; other
// surfaces (e.g. the Google review prompt) react to them.

export type PaymentStatus =
  'PENDING' | 'APPROVED' | 'DECLINED' | 'CANCELED' | 'REFUNDED'
