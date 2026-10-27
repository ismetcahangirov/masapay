import { api } from '@/app/api'

// The restaurant menu (#137): categories in the manager's order, each with its
// dishes. Every staff member reads it; managers and super admins change it.
// Prices are in manat, as the backend sends them.

/** One dish inside a combo and how many of it (#138). */
export interface ComboComponent {
  itemId: string
  name: string
  quantity: number
}

export interface MenuDish {
  id: string
  categoryId: string
  name: string
  description: string | null
  price: number
  /** A combo's contents, such as a business lunch; empty for an ordinary dish. */
  components: ComboComponent[]
  /** Daily menu settings (#139); days are ISO dates in Baku time. */
  active: boolean
  offOn: string | null
  availableFrom: string | null
  availableUntil: string | null
  /** Whether customers can order it right now. */
  availableToday: boolean
  /** Extras offered with it before the customer confirms (#141), in order. */
  suggestionIds: string[]
  /** Every customer is asked about it before confirming, e.g. ketchup (#185). */
  askBeforeConfirm: boolean
  /** The dish photo customers see, or null; the URL changes with each new photo. */
  imageUrl: string | null
}

export type AvailabilityMode = 'ALWAYS' | 'OFF' | 'OFF_TODAY' | 'DATES'

export interface AvailabilityInput {
  mode: AvailabilityMode
  from: string | null
  until: string | null
}

export interface MenuCategory {
  id: string
  name: string
  sortOrder: number
  items: MenuDish[]
}

export interface CategoryInput {
  name: string
  sortOrder: number
}

export interface DishInput {
  categoryId: string
  name: string
  description: string | null
  price: number
  /** Replaces a combo's contents; empty for an ordinary dish. */
  components: { itemId: string; quantity: number }[]
}

/**
 * A dish of a menu read from a PDF or Word file, as the manager reviews it.
 * `image` is a data: URL; `existingId` is the dish of the same name already on
 * the menu, which the import updates instead of adding a second one.
 */
export interface ImportDish {
  name: string
  description: string | null
  price: number
  image: string | null
  existingId: string | null
}

export interface ImportCategory {
  name: string
  items: ImportDish[]
  /** The menu already has a category of this name; the dishes join it. */
  existing: boolean
}

export interface ImportDraft {
  categories: ImportCategory[]
  /** Pictures in the file that were not JPEG, PNG or WebP photos. */
  skippedImages: number
  /** The text was recognised from pictures (a scan or a photo): check closely. */
  ocr: boolean
}

export interface ImportResult {
  categoriesCreated: number
  dishesCreated: number
  dishesUpdated: number
}

const menuUrl = (restaurantId: string) =>
  `/admin/restaurants/${restaurantId}/menu`

export const menuApi = api.injectEndpoints({
  endpoints: (build) => ({
    restaurantMenu: build.query<MenuCategory[], string>({
      query: (restaurantId) => menuUrl(restaurantId),
      providesTags: (_r, _e, restaurantId) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    createCategory: build.mutation<
      MenuCategory,
      { restaurantId: string; body: CategoryInput }
    >({
      query: ({ restaurantId, body }) => ({
        url: `${menuUrl(restaurantId)}/categories`,
        method: 'POST',
        body,
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    updateCategory: build.mutation<
      MenuCategory,
      { restaurantId: string; categoryId: string; body: CategoryInput }
    >({
      query: ({ restaurantId, categoryId, body }) => ({
        url: `${menuUrl(restaurantId)}/categories/${categoryId}`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    deleteCategory: build.mutation<
      void,
      { restaurantId: string; categoryId: string }
    >({
      query: ({ restaurantId, categoryId }) => ({
        url: `${menuUrl(restaurantId)}/categories/${categoryId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    createDish: build.mutation<
      MenuDish,
      { restaurantId: string; body: DishInput }
    >({
      query: ({ restaurantId, body }) => ({
        url: `${menuUrl(restaurantId)}/items`,
        method: 'POST',
        body,
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    updateDish: build.mutation<
      MenuDish,
      { restaurantId: string; dishId: string; body: DishInput }
    >({
      query: ({ restaurantId, dishId, body }) => ({
        url: `${menuUrl(restaurantId)}/items/${dishId}`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    setSuggestions: build.mutation<
      MenuDish,
      { restaurantId: string; dishId: string; itemIds: string[] }
    >({
      query: ({ restaurantId, dishId, itemIds }) => ({
        url: `${menuUrl(restaurantId)}/items/${dishId}/suggestions`,
        method: 'PUT',
        body: { itemIds },
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    setExtra: build.mutation<
      MenuDish,
      { restaurantId: string; dishId: string; askBeforeConfirm: boolean }
    >({
      query: ({ restaurantId, dishId, askBeforeConfirm }) => ({
        url: `${menuUrl(restaurantId)}/items/${dishId}/extra`,
        method: 'PUT',
        body: { askBeforeConfirm },
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    setAvailability: build.mutation<
      MenuDish,
      { restaurantId: string; dishId: string; body: AvailabilityInput }
    >({
      query: ({ restaurantId, dishId, body }) => ({
        url: `${menuUrl(restaurantId)}/items/${dishId}/availability`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    setDishImage: build.mutation<
      MenuDish,
      { restaurantId: string; dishId: string; file: Blob }
    >({
      query: ({ restaurantId, dishId, file }) => {
        const body = new FormData()
        body.append('file', file)
        return {
          url: `${menuUrl(restaurantId)}/items/${dishId}/image`,
          method: 'PUT',
          body,
        }
      },
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    deleteDishImage: build.mutation<
      MenuDish,
      { restaurantId: string; dishId: string }
    >({
      query: ({ restaurantId, dishId }) => ({
        url: `${menuUrl(restaurantId)}/items/${dishId}/image`,
        method: 'DELETE',
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    /** Reads a PDF or Word menu; nothing is saved until applyImport. */
    previewImport: build.mutation<
      ImportDraft,
      { restaurantId: string; file: Blob }
    >({
      query: ({ restaurantId, file }) => {
        const body = new FormData()
        body.append('file', file)
        return {
          url: `${menuUrl(restaurantId)}/import/preview`,
          method: 'POST',
          body,
        }
      },
    }),
    applyImport: build.mutation<
      ImportResult,
      { restaurantId: string; categories: ImportCategory[] }
    >({
      query: ({ restaurantId, categories }) => ({
        url: `${menuUrl(restaurantId)}/import`,
        method: 'POST',
        body: { categories },
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
    deleteDish: build.mutation<void, { restaurantId: string; dishId: string }>({
      query: ({ restaurantId, dishId }) => ({
        url: `${menuUrl(restaurantId)}/items/${dishId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Menu', id: restaurantId },
      ],
    }),
  }),
})

export const {
  useRestaurantMenuQuery,
  useCreateCategoryMutation,
  useUpdateCategoryMutation,
  useDeleteCategoryMutation,
  useCreateDishMutation,
  useUpdateDishMutation,
  useSetAvailabilityMutation,
  useSetExtraMutation,
  useSetSuggestionsMutation,
  useDeleteDishMutation,
  useSetDishImageMutation,
  useDeleteDishImageMutation,
  usePreviewImportMutation,
  useApplyImportMutation,
} = menuApi
