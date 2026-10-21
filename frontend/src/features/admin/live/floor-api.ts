import { api } from '@/app/api'
import { subscribeToTopic } from '@/lib/stomp'
import type { RootState } from '@/app/store'

// Live floor plan (#44): every table of a restaurant with its bill status.
// Amounts arrive as decimals (12.5); null when the table has no bill.

export type FloorStatus = 'EMPTY' | 'ACTIVE' | 'PARTIALLY_PAID' | 'CLOSED'

export interface FloorTable {
  tableId: string
  label: string
  status: FloorStatus
  currency: string
  totalAmount: number | null
  paidAmount: number | null
  outstanding: number | null
  /** When the bill was opened (ISO time), null for an empty table. */
  openedAt: string | null
  /** Minutes since the bill opened, only while it is still open. */
  minutesOpen: number | null
}

/** Replaces the one changed table in the list (or appends a new table). */
export function patchFloor(floor: FloorTable[], entry: FloorTable) {
  const index = floor.findIndex((table) => table.tableId === entry.tableId)
  if (index === -1) floor.push(entry)
  else floor[index] = entry
}

export const floorApi = api.injectEndpoints({
  endpoints: (build) => ({
    restaurantFloor: build.query<FloorTable[], string>({
      query: (restaurantId) => `/admin/restaurants/${restaurantId}/floor`,
      providesTags: (_r, _e, restaurantId) => [
        { type: 'Floor', id: restaurantId },
      ],
      // While the page is open, keep a STOMP subscription to the restaurant's
      // floor topic. Every bill change pushes just that table's entry, which is
      // patched into the cached list, so the tile changes colour at once.
      async onCacheEntryAdded(
        restaurantId,
        {
          updateCachedData,
          cacheDataLoaded,
          cacheEntryRemoved,
          dispatch,
          getState,
        },
      ) {
        try {
          await cacheDataLoaded
        } catch {
          return // the first load failed; nothing to keep live
        }
        let connectedBefore = false
        const subscription = subscribeToTopic(
          `/topic/restaurants/${restaurantId}/floor`,
          (body) => {
            try {
              const entry = JSON.parse(body) as FloorTable
              updateCachedData((floor) => patchFloor(floor, entry))
            } catch {
              // Ignore a malformed push; the next one or a refetch fixes it.
            }
          },
          () => {
            // After a reconnect, refetch once to pick up anything missed.
            if (connectedBefore) {
              dispatch(
                floorApi.util.invalidateTags([
                  { type: 'Floor', id: restaurantId },
                ]),
              )
            }
            connectedBefore = true
          },
          // The floor topic is staff-only: send the in-memory access token (#44).
          () => (getState() as RootState).auth.accessToken,
        )
        await cacheEntryRemoved
        subscription.close()
      },
    }),
  }),
})

export const { useRestaurantFloorQuery } = floorApi
