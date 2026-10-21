import { api } from '@/app/api'
import { toMinor } from '@/features/order/order-api'

// Customer payment endpoints (#24 full, #25 own items, #26 equal split), all
// reached with the table's qrToken. Each mode has a quote (GET: "how much would
// I pay?") and a checkout (POST) that opens a Payriff payment (#37): the guest is
// sent to Payriff's page and the split is applied once Payriff confirms it.

export interface Quote {
  /** Amount to pay, in minor units. */
  amount: number
  currency: string
}

export interface EqualQuote extends Quote {
  totalParts: number
  paidParts: number
}

/** A started payment: the pending transaction and the Payriff page to open. */
export interface Checkout {
  transactionId: string
  paymentUrl: string
}

export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED'

export interface PaymentStatusView {
  transactionId: string
  status: PaymentStatus
  /** Charged amount in minor units. */
  amount: number
  currency: string
  orderStatus: string
  restaurantId: string
  tableId: string
  qrToken: string
}

interface QuoteDto {
  amount: number
  currency: string
}

interface EqualQuoteDto extends QuoteDto {
  totalParts: number
  paidParts: number
}

interface PaymentStatusDto extends Omit<PaymentStatusView, 'amount'> {
  amount: number
}

const toQuote = (dto: QuoteDto): Quote => ({
  amount: toMinor(dto.amount),
  currency: dto.currency,
})

const base = (qrToken: string) =>
  `/public/tables/${encodeURIComponent(qrToken)}/payment`

export const paymentApi = api.injectEndpoints({
  endpoints: (build) => ({
    quoteFull: build.query<Quote, string>({
      query: (qrToken) => `${base(qrToken)}/full`,
      transformResponse: toQuote,
      keepUnusedDataFor: 0,
    }),
    checkoutFull: build.mutation<Checkout, string>({
      query: (qrToken) => ({
        url: `${base(qrToken)}/full/checkout`,
        method: 'POST',
      }),
    }),

    quoteEqual: build.query<
      EqualQuote,
      { qrToken: string; parts: number; partsToPay: number }
    >({
      query: ({ qrToken, parts, partsToPay }) => ({
        url: `${base(qrToken)}/equal`,
        params: { parts, partsToPay },
      }),
      transformResponse: (dto: EqualQuoteDto) => ({
        ...toQuote(dto),
        totalParts: dto.totalParts,
        paidParts: dto.paidParts,
      }),
      keepUnusedDataFor: 0,
    }),
    checkoutEqual: build.mutation<
      Checkout,
      { qrToken: string; parts: number; partsToPay: number }
    >({
      query: ({ qrToken, parts, partsToPay }) => ({
        url: `${base(qrToken)}/equal/checkout`,
        method: 'POST',
        body: { parts, partsToPay },
      }),
    }),

    quoteItems: build.query<
      Quote,
      { qrToken: string; itemIds: string[]; guests?: number }
    >({
      query: ({ qrToken, itemIds, guests }) => {
        const params = new URLSearchParams()
        itemIds.forEach((id) => params.append('itemIds', id))
        if (guests) params.set('guests', String(guests))
        return `${base(qrToken)}/items?${params.toString()}`
      },
      transformResponse: toQuote,
      keepUnusedDataFor: 0,
    }),
    checkoutItems: build.mutation<
      Checkout,
      { qrToken: string; itemIds: string[]; guests?: number }
    >({
      query: ({ qrToken, itemIds, guests }) => ({
        url: `${base(qrToken)}/items/checkout`,
        method: 'POST',
        body: { itemIds, guests: guests ?? null },
      }),
    }),

    /** Status of a payment, polled by the result page Payriff returns to. */
    paymentStatus: build.query<PaymentStatusView, string>({
      query: (transactionId) =>
        `/public/payments/${encodeURIComponent(transactionId)}`,
      transformResponse: (dto: PaymentStatusDto) => ({
        ...dto,
        amount: toMinor(dto.amount),
      }),
    }),
  }),
})

export const {
  useQuoteFullQuery,
  useCheckoutFullMutation,
  useQuoteEqualQuery,
  useCheckoutEqualMutation,
  useQuoteItemsQuery,
  useCheckoutItemsMutation,
  usePaymentStatusQuery,
} = paymentApi

/** Sends the guest to Payriff's payment page (a full page load, as Payriff requires). */
export function goToPayment(checkout: Checkout) {
  window.location.assign(checkout.paymentUrl)
}
