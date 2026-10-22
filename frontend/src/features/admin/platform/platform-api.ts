import { api } from '@/app/api'

// The platform's restaurants, run by the super admin (#147).

export type PosType = 'IIKO' | 'MICROS' | 'CUSTOM'

export interface PlatformManager {
  id: string
  email: string
  name: string | null
  /** False until the invited manager has signed in with Google once. */
  signedIn: boolean
}

export interface PlatformRestaurant {
  id: string
  name: string
  currency: string
  posType: PosType
  active: boolean
  createdAt: string
  managers: PlatformManager[]
}

export interface NewRestaurant {
  name: string
  currency: string
  posType: PosType
  managerEmail: string
}

const URL = '/admin/platform/restaurants'
const LIST = [{ type: 'Platform' as const, id: 'restaurants' }]

export const platformApi = api.injectEndpoints({
  endpoints: (build) => ({
    platformRestaurants: build.query<PlatformRestaurant[], void>({
      query: () => URL,
      providesTags: LIST,
    }),
    createRestaurant: build.mutation<PlatformRestaurant, NewRestaurant>({
      query: (body) => ({ url: URL, method: 'POST', body }),
      // Same tag as the restaurant pickers, so they show the new one too.
      invalidatesTags: LIST,
    }),
    updateRestaurant: build.mutation<
      PlatformRestaurant,
      { id: string; name?: string; active?: boolean }
    >({
      query: ({ id, ...body }) => ({
        url: `${URL}/${id}`,
        method: 'PATCH',
        body,
      }),
      invalidatesTags: LIST,
    }),
    addManager: build.mutation<
      PlatformRestaurant,
      { id: string; email: string }
    >({
      query: ({ id, email }) => ({
        url: `${URL}/${id}/managers`,
        method: 'POST',
        body: { email },
      }),
      invalidatesTags: LIST,
    }),
  }),
})

export const {
  usePlatformRestaurantsQuery,
  useCreateRestaurantMutation,
  useUpdateRestaurantMutation,
  useAddManagerMutation,
} = platformApi
