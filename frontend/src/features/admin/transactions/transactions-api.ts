import { api } from '@/app/api'

// Admin transactions table (#45): every payment of a restaurant, searchable
// and paged on the server, with a one-click full refund.

export type TransactionStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED'
export type SplitType = 'FULL' | 'ITEM' | 'EQUAL'

export interface TransactionRow {
  id: string
  createdAt: string
  paidAt: string | null
  tableLabel: string
  splitType: SplitType | null
  status: TransactionStatus
  /** Amounts in major units (manat), e.g. 44.5. */
  amount: number
  tipAmount: number
  totalAmount: number
  refundedAmount: number
  currency: string
  payriffOrderId: string | null
  /** True only for a successful payment that went through Payriff. */
  refundable: boolean
}

export interface TransactionPage {
  items: TransactionRow[]
  totalElements: number
  /** Zero-based page number. */
  page: number
  size: number
}

export interface TransactionSearch {
  restaurantId: string
  status?: TransactionStatus
  splitType?: SplitType
  /** Inclusive ISO dates (YYYY-MM-DD), Baku time. */
  from?: string
  to?: string
  q?: string
  page: number
  size: number
}

interface RefundResult {
  transactionId: string
  status: TransactionStatus
  refundedAmount: number
  currency: string
}

export const transactionsApi = api.injectEndpoints({
  endpoints: (build) => ({
    restaurantTransactions: build.query<TransactionPage, TransactionSearch>({
      query: ({ restaurantId, ...params }) => ({
        url: `/admin/restaurants/${restaurantId}/transactions`,
        // Empty filters are left out of the query string.
        params: Object.fromEntries(
          Object.entries(params).filter(
            ([, value]) => value !== undefined && value !== '',
          ),
        ),
      }),
      providesTags: (_r, _e, { restaurantId }) => [
        { type: 'Transactions', id: restaurantId },
      ],
    }),
    refundTransaction: build.mutation<
      RefundResult,
      { restaurantId: string; transactionId: string; reason?: string }
    >({
      query: ({ transactionId, reason }) => ({
        url: `/admin/transactions/${transactionId}/refund`,
        method: 'POST',
        body: { reason: reason || null },
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Transactions', id: restaurantId },
      ],
    }),
  }),
})

export const { useRestaurantTransactionsQuery, useRefundTransactionMutation } =
  transactionsApi
