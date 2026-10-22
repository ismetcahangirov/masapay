import { api } from '@/app/api'

// A restaurant's waiters and chefs, run by its manager (#145).

export type StaffRole = 'WAITER' | 'CHEF'

export type StaffStatus = 'INVITED' | 'PENDING' | 'ACTIVE' | 'DEACTIVATED'

export interface StaffMember {
  id: string
  email: string
  name: string | null
  role: StaffRole
  status: StaffStatus
  lastLoginAt: string | null
}

const staffUrl = (restaurantId: string) =>
  `/admin/restaurants/${restaurantId}/staff`

export const staffApi = api.injectEndpoints({
  endpoints: (build) => ({
    restaurantStaff: build.query<StaffMember[], string>({
      query: (restaurantId) => staffUrl(restaurantId),
      providesTags: (_r, _e, restaurantId) => [
        { type: 'Staff', id: restaurantId },
      ],
    }),
    inviteStaff: build.mutation<
      StaffMember,
      { restaurantId: string; email: string; role: StaffRole }
    >({
      query: ({ restaurantId, email, role }) => ({
        url: staffUrl(restaurantId),
        method: 'POST',
        body: { email, role },
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Staff', id: restaurantId },
      ],
    }),
    updateStaff: build.mutation<
      StaffMember,
      {
        restaurantId: string
        userId: string
        role?: StaffRole
        active?: boolean
      }
    >({
      query: ({ restaurantId, userId, role, active }) => ({
        url: `${staffUrl(restaurantId)}/${userId}`,
        method: 'PATCH',
        body: { role, active },
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Staff', id: restaurantId },
      ],
    }),
  }),
})

export const {
  useRestaurantStaffQuery,
  useInviteStaffMutation,
  useUpdateStaffMutation,
} = staffApi
