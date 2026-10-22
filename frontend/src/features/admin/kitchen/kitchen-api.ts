import { api } from '@/app/api'
import type { RootState } from '@/app/store'
import { subscribeToTopic } from '@/lib/stomp'

// Kitchen screen (#142): the dishes customers ordered, oldest first, with
// where each one is. NEW until the chef starts it, then PREPARING and READY.

export type KitchenStatus = 'NEW' | 'PREPARING' | 'READY'

export interface KitchenLine {
  id: string
  tableId: string
  tableLabel: string
  name: string
  quantity: number
  status: KitchenStatus
  /** When the customer confirmed it (ISO time). */
  orderedAt: string
}

const kitchenUrl = (restaurantId: string) =>
  `/admin/restaurants/${restaurantId}/kitchen`

export const kitchenApi = api.injectEndpoints({
  endpoints: (build) => ({
    kitchenWork: build.query<KitchenLine[], string>({
      query: (restaurantId) => kitchenUrl(restaurantId),
      providesTags: (_r, _e, restaurantId) => [
        { type: 'Kitchen', id: restaurantId },
      ],
      // While the screen is open, listen to the restaurant's kitchen topic. A
      // message only says which table changed (a new order, or another chef
      // marked a dish), so the list is simply loaded again.
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
            kitchenApi.util.invalidateTags([
              { type: 'Kitchen', id: restaurantId },
            ]),
          )
        let connectedBefore = false
        const subscription = subscribeToTopic(
          `/topic/restaurants/${restaurantId}/kitchen`,
          reload,
          () => {
            // After a reconnect, reload once to pick up anything missed.
            if (connectedBefore) reload()
            connectedBefore = true
          },
          // The kitchen topic is staff-only: send the in-memory access token.
          () => (getState() as RootState).auth.accessToken,
        )
        await cacheEntryRemoved
        subscription.close()
      },
    }),
    markKitchenLine: build.mutation<
      KitchenLine,
      { restaurantId: string; lineId: string; status: KitchenStatus }
    >({
      query: ({ restaurantId, lineId, status }) => ({
        url: `${kitchenUrl(restaurantId)}/items/${lineId}`,
        method: 'PUT',
        body: { status },
      }),
      // Show the new status at once; the live ping then reloads the list anyway.
      async onQueryStarted(
        { restaurantId, lineId, status },
        { dispatch, queryFulfilled },
      ) {
        const patch = dispatch(
          kitchenApi.util.updateQueryData(
            'kitchenWork',
            restaurantId,
            (list) => {
              const line = list.find((l) => l.id === lineId)
              if (line) line.status = status
            },
          ),
        )
        try {
          await queryFulfilled
        } catch {
          patch.undo()
        }
      },
    }),
  }),
})

export const { useKitchenWorkQuery, useMarkKitchenLineMutation } = kitchenApi
