import { api } from '@/app/api'

// Restaurant tables and their QR codes (#47). Every staff member sees their
// own restaurant's tables; managers and super admins can add tables.

export interface AdminTable {
  id: string
  label: string
  status: string
  qrToken: string
  /** The customer link the table's QR code encodes. */
  customerUrl: string
}

export const tablesApi = api.injectEndpoints({
  endpoints: (build) => ({
    restaurantTables: build.query<AdminTable[], string>({
      query: (restaurantId) => `/admin/restaurants/${restaurantId}/tables`,
      providesTags: (_r, _e, restaurantId) => [
        { type: 'Tables', id: restaurantId },
      ],
    }),
    createTable: build.mutation<
      AdminTable,
      { restaurantId: string; label: string }
    >({
      query: ({ restaurantId, label }) => ({
        url: `/admin/restaurants/${restaurantId}/tables`,
        method: 'POST',
        body: { label },
      }),
      invalidatesTags: (_r, _e, { restaurantId }) => [
        { type: 'Tables', id: restaurantId },
      ],
    }),
    /** The table's print-ready QR code as SVG markup. */
    tableQrSvg: build.query<string, string>({
      query: (tableId) => ({
        url: `/admin/tables/${tableId}/qr`,
        responseHandler: 'text',
      }),
    }),
    /** All of a restaurant's QR codes as a ZIP of SVG files. */
    restaurantQrZip: build.mutation<Blob, string>({
      query: (restaurantId) => ({
        url: `/admin/restaurants/${restaurantId}/tables/qr`,
        responseHandler: (response) => response.blob(),
      }),
    }),
  }),
})

export const {
  useRestaurantTablesQuery,
  useCreateTableMutation,
  useTableQrSvgQuery,
  useRestaurantQrZipMutation,
} = tablesApi

/** Lets the browser save a file the API returned (the request itself needs the access token). */
export function saveFile(data: Blob, fileName: string) {
  const url = URL.createObjectURL(data)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}
