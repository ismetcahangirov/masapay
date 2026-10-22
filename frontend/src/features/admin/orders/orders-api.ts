import { api } from '@/app/api'
import type { RootState } from '@/app/store'
import { toTableOrder, type TableOrderDto } from '@/features/order/order-api'
import type { TableOrder } from '@/features/order/types'
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
        const reload = () =>
          dispatch(
            ordersApi.util.invalidateTags([
              { type: 'Bills', id: restaurantId },
            ]),
          )
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
  }),
})

export const { useRestaurantBillsQuery } = ordersApi
