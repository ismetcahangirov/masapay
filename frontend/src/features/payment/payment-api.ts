import { api } from '@/app/api'
import { toMinor } from '@/features/order/order-api'

// Customer payment endpoints (#24 full, #25 own items, #26 equal split), all
// reached with the table's qrToken. Each mode has a quote (GET: "how much would
// I pay?") and a pay call (POST). Amounts come back as decimals and are turned
// into minor units here, like the bill.

export interface Quote {
  /** Amount to pay, in minor units. */
  amount: number
  currency: string
}

export interface EqualQuote extends Quote {
  totalParts: number
  paidParts: number
}

export interface Receipt {
  transactionId: string
  amount: number
  currency: string
  /** Order status after the payment, e.g. "OPEN" or "PAID". */
  orderStatus: string
}

interface QuoteDto {
  amount: number
  currency: string
}

interface EqualQuoteDto extends QuoteDto {
  totalParts: number
  paidParts: number
}

interface ReceiptDto {
  transactionId: string
  amount: number
  currency: string
  orderStatus: string
}

const toQuote = (dto: QuoteDto): Quote => ({
  amount: toMinor(dto.amount),
  currency: dto.currency,
})

const toReceipt = (dto: ReceiptDto): Receipt => ({
  transactionId: dto.transactionId,
  amount: toMinor(dto.amount),
  currency: dto.currency,
  orderStatus: dto.orderStatus,
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
    payFull: build.mutation<Receipt, string>({
      query: (qrToken) => ({ url: `${base(qrToken)}/full`, method: 'POST' }),
      transformResponse: toReceipt,
      invalidatesTags: (_r, _e, qrToken) => [
        { type: 'TableOrder', id: qrToken },
      ],
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
    payEqual: build.mutation<
      Receipt,
      { qrToken: string; parts: number; partsToPay: number }
    >({
      query: ({ qrToken, parts, partsToPay }) => ({
        url: `${base(qrToken)}/equal`,
        method: 'POST',
        body: { parts, partsToPay },
      }),
      transformResponse: toReceipt,
      invalidatesTags: (_r, _e, { qrToken }) => [
        { type: 'TableOrder', id: qrToken },
      ],
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
    payItems: build.mutation<
      Receipt,
      { qrToken: string; itemIds: string[]; guests?: number }
    >({
      query: ({ qrToken, itemIds, guests }) => ({
        url: `${base(qrToken)}/items`,
        method: 'POST',
        body: { itemIds, guests: guests ?? null },
      }),
      transformResponse: toReceipt,
      invalidatesTags: (_r, _e, { qrToken }) => [
        { type: 'TableOrder', id: qrToken },
      ],
    }),
  }),
})

export const {
  useQuoteFullQuery,
  usePayFullMutation,
  useQuoteEqualQuery,
  usePayEqualMutation,
  useQuoteItemsQuery,
  usePayItemsMutation,
} = paymentApi
