import { api } from '@/app/api'

// The platform's restaurants and their subscriptions, run by the super admin
// (#147, #148).

export type PosType = 'IIKO' | 'MICROS' | 'CUSTOM'

export interface PlatformManager {
  id: string
  email: string
  name: string | null
  /** False until the invited manager has signed in with Google once. */
  signedIn: boolean
}

export type SubscriptionPlan = 'BASIC' | 'PRO'
/** EXPIRED: after the last day; the restaurant's customers cannot pay online. */
export type SubscriptionStatus = 'NONE' | 'TRIAL' | 'ACTIVE' | 'EXPIRED'

/** Dates are "YYYY-MM-DD", both days included, Baku time. */
export interface Subscription {
  plan: SubscriptionPlan
  trial: boolean
  startsOn: string
  endsOn: string
  monthlyFee: number
}

export interface SubscriptionView extends Partial<Omit<Subscription, 'trial'>> {
  status: SubscriptionStatus
  trial: boolean
}

export interface PlatformRestaurant {
  id: string
  name: string
  currency: string
  posType: PosType
  active: boolean
  createdAt: string
  managers: PlatformManager[]
  subscription: SubscriptionView
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
    setSubscription: build.mutation<
      PlatformRestaurant,
      { id: string } & Subscription
    >({
      query: ({ id, ...body }) => ({
        url: `${URL}/${id}/subscription`,
        method: 'PUT',
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
  useSetSubscriptionMutation,
  useAddManagerMutation,
} = platformApi
