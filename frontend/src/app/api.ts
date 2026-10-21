import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

// Single RTK Query API for the whole app. Feature slices add their endpoints
// with `api.injectEndpoints` so each feature owns its own requests.
export const api = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
  tagTypes: ['TableOrder'],
  endpoints: () => ({}),
})
