import { api } from '@/app/api'
import type { RootState } from '@/app/store'
import { toTableOrder, type TableOrderDto } from '@/features/order/order-api'
import type { TableOrder } from '@/features/order/types'
import type {
  SplitType,
  TransactionStatus,
} from '@/features/admin/transactions/transactions-api'
import { subscribeToTopic } from '@/lib/stomp'

// Staff orders screen (#143): the full bill of every table that has one, in
// the same shape as the customer's bill, so both always show the same thing.

export const ordersApi = api.injectEndpoints({
  endpoints: (build) => ({
    restaurantBills: build.query<TableOrder[], string>({
      query: (restaurantId) => `/admin/restaurants/${restaurantId}/floor/bills`,
      transformResponse: (dto: TableOrderDto[]) => dto.map(toTableOrder),
      providesTags: (_r, _e, restaurantId) => [
        { type: 'Bills', id: restaurantId },
      ],
      // Every bill change (an order, a payment, a dish marked ready) already
      // updates the live floor; on each such push the bills are loaded again.
      async onCacheEntryAdded(
        restaurantId,
        { cacheDataLoaded, cacheEntryRemoved, dispatch, getState },
      ) {
        try {
          await cacheDataLoaded
        } catch {
          return // the first load failed; nothing to keep live
        }
        // Without an id this also reloads any open table detail (its payments).
        const reload = () => dispatch(ordersApi.util.invalidateTags(['Bills']))
        let connectedBefore = false
        const subscription = subscribeToTopic(
          `/topic/restaurants/${restaurantId}/floor`,
          reload,
          () => {
            if (connectedBefore) reload()
            connectedBefore = true
          },
          // Staff-only topic: send the in-memory access token.
          () => (getState() as RootState).auth.accessToken,
        )
        await cacheEntryRemoved
        subscription.close()
      },
    }),
    /** Payments on the table's current bill, newest first (#144). Amounts in manat. */
    tablePayments: build.query<TablePayment[], string>({
      query: (tableId) => `/admin/tables/${tableId}/order/payments`,
      providesTags: (_r, _e, tableId) => [
        { type: 'Bills', id: `pay-${tableId}` },
      ],
    }),
    /** A waiter takes the table: they serve it and get its tips (#182). */
    claimTable: build.mutation<
      TableOrderDto,
      { tableId: string; restaurantId: string }
    >({
      query: ({ tableId }) => ({
        url: `/admin/tables/${tableId}/order/claim`,
        method: 'POST',
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Bills', id: restaurantId },
      ],
    }),
    /** Staff add a dish from today's menu; it goes to the kitchen (#144). */
    addMenuDish: build.mutation<
      TableOrderDto,
      {
        tableId: string
        restaurantId: string
        menuItemId: string
        quantity: number
      }
    >({
      query: ({ tableId, menuItemId, quantity }) => ({
        url: `/admin/tables/${tableId}/order/menu-items`,
        method: 'POST',
        body: { menuItemId, quantity },
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Bills', id: restaurantId },
      ],
    }),
  }),
})

export interface TablePayment {
  id: string
  amount: number
  tipAmount: number
  totalAmount: number
  refundedAmount: number
  splitType: SplitType | null
  status: TransactionStatus
  createdAt: string
}

export const {
  useClaimTableMutation,
  useRestaurantBillsQuery,
  useTablePaymentsQuery,
  useAddMenuDishMutation,
} = ordersApi
