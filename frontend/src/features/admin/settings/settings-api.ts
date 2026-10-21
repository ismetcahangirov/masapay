import { api } from '@/app/api'

// Admin restaurant settings (#46): POS provider, iiko/Payriff credentials and
// the service charge. Secrets come back masked (e.g. "••••••••abcd") and are
// never sent to the browser in clear text.

export type PosType = 'IIKO' | 'MICROS' | 'CUSTOM'

export interface RestaurantSummary {
  id: string
  name: string
}

export interface IntegrationSettings {
  restaurantId: string
  posType: PosType
  iikoApiLogin: string | null
  iikoApiLoginSet: boolean
  iikoOrganizationId: string | null
  iikoPaymentTypeId: string | null
  payriffMerchantId: string | null
  payriffSecretKey: string | null
  payriffSecretKeySet: boolean
  serviceChargePercent: number | null
}

/** Partial update: a field left undefined (or null) is not changed. */
export interface UpdateIntegrationSettings {
  posType?: PosType
  iikoApiLogin?: string
  iikoOrganizationId?: string
  iikoPaymentTypeId?: string
  payriffMerchantId?: string
  payriffSecretKey?: string
  serviceChargeEnabled?: boolean
  serviceChargePercent?: number
}

export const settingsApi = api.injectEndpoints({
  endpoints: (build) => ({
    /** Restaurants this admin may manage (all for a super admin). */
    myRestaurants: build.query<RestaurantSummary[], void>({
      query: () => '/admin/restaurants',
    }),
    integrationSettings: build.query<IntegrationSettings, string>({
      query: (restaurantId) =>
        `/admin/restaurants/${restaurantId}/integration-settings`,
      providesTags: (_r, _e, restaurantId) => [
        { type: 'Settings', id: restaurantId },
      ],
    }),
    updateIntegrationSettings: build.mutation<
      IntegrationSettings,
      { restaurantId: string; body: UpdateIntegrationSettings }
    >({
      query: ({ restaurantId, body }) => ({
        url: `/admin/restaurants/${restaurantId}/integration-settings`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Settings', id: restaurantId },
      ],
    }),
  }),
})

export const {
  useMyRestaurantsQuery,
  useIntegrationSettingsQuery,
  useUpdateIntegrationSettingsMutation,
} = settingsApi
