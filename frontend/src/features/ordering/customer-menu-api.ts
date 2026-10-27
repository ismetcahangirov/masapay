import { api } from '@/app/api'
import {
  orderApi,
  toMinor,
  toTableOrder,
  type TableOrderDto,
} from '@/features/order/order-api'

// The customer's side of the menu (#140): what can be ordered today at this
// table, and confirming a basket onto the table's bill. Prices arrive in manat
// and are turned into minor units here, like the bill.

export interface CustomerDish {
  id: string
  name: string
  description: string | null
  /** Minor units (qəpik). */
  price: number
  /** A combo's contents, e.g. "1 x Mərci"; empty for an ordinary dish. */
  components: { name: string; quantity: number }[]
  /** Extras the restaurant offers with it before confirming (#141). */
  suggestionIds: string[]
  /** Asked about before every order is confirmed, e.g. ketchup or water (#185). */
  askBeforeConfirm: boolean
  /** The dish photo, or null when the restaurant has not added one. */
  imageUrl: string | null
}

export interface CustomerCategory {
  id: string
  name: string
  items: CustomerDish[]
}

interface CategoryDto {
  id: string
  name: string
  items: {
    id: string
    name: string
    description: string | null
    price: number
    components: { name: string; quantity: number }[]
    suggestionIds?: string[]
    askBeforeConfirm?: boolean
    imageUrl?: string | null
  }[]
}

export interface BasketLine {
  menuItemId: string
  quantity: number
}

const tokenPath = (qrToken: string) =>
  `/public/tables/${encodeURIComponent(qrToken)}`

export const customerMenuApi = api.injectEndpoints({
  endpoints: (build) => ({
    customerMenu: build.query<CustomerCategory[], string>({
      query: (qrToken) => `${tokenPath(qrToken)}/menu`,
      transformResponse: (dto: CategoryDto[]) =>
        dto.map((category) => ({
          id: category.id,
          name: category.name,
          items: category.items.map((item) => ({
            id: item.id,
            name: item.name,
            description: item.description,
            price: toMinor(item.price),
            components: item.components.map(({ name, quantity }) => ({
              name,
              quantity,
            })),
            suggestionIds: item.suggestionIds ?? [],
            askBeforeConfirm: item.askBeforeConfirm ?? false,
            imageUrl: item.imageUrl ?? null,
          })),
        })),
      providesTags: (_r, _e, qrToken) => [
        { type: 'CustomerMenu', id: qrToken },
      ],
    }),
    /** Puts the confirmed basket onto the bill; the answer is the fresh bill. */
    placeOrder: build.mutation<
      TableOrderDto,
      { qrToken: string; items: BasketLine[] }
    >({
      query: ({ qrToken, items }) => ({
        url: `${tokenPath(qrToken)}/order/items`,
        method: 'POST',
        body: { items },
      }),
      async onQueryStarted({ qrToken }, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled
          // Show the new lines at once instead of waiting for the live push.
          dispatch(
            orderApi.util.updateQueryData('getTableOrder', qrToken, () =>
              toTableOrder(data),
            ),
          )
        } catch {
          // A dish went off the menu meanwhile: load today's menu again.
          dispatch(
            customerMenuApi.util.invalidateTags([
              { type: 'CustomerMenu', id: qrToken },
            ]),
          )
        }
      },
    }),
  }),
})

export const { useCustomerMenuQuery, usePlaceOrderMutation } = customerMenuApi
