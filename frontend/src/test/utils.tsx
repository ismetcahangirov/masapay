import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import { store } from '@/app/store'

/** Renders a component with the app's Redux store and a router, like main.tsx does. */
export function renderWithProviders(ui: ReactElement, { route = '/' } = {}) {
  return render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </Provider>,
  )
}

type Handler = (request: Request) => unknown | Promise<unknown>

/**
 * Replaces fetch with a tiny fake backend: `routes` maps "METHOD /path" to a
 * JSON body (or a function of the request). Unknown routes return 404. Returns
 * the mock so a test can inspect the requests that were made.
 */
export function mockApi(routes: Record<string, unknown | Handler>) {
  const fetchMock = vi.fn(
    async (input: RequestInfo | URL, init?: RequestInit) => {
      const request =
        input instanceof Request ? input : new Request(input, init)
      const url = new URL(request.url)
      const key = `${request.method} ${url.pathname}`
      if (!(key in routes)) {
        return new Response(JSON.stringify({ status: 404 }), { status: 404 })
      }
      const route = routes[key]
      const body =
        typeof route === 'function' ? await (route as Handler)(request) : route
      if (body instanceof Response) return body
      const text = typeof body === 'string' ? body : JSON.stringify(body)
      return new Response(text, {
        status: 200,
        headers: {
          'Content-Type':
            typeof body === 'string' ? 'text/plain' : 'application/json',
        },
      })
    },
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** The requests the fake backend received, as "METHOD /path?query". */
export function requestsOf(fetchMock: ReturnType<typeof mockApi>): string[] {
  return fetchMock.mock.calls.map(([input, init]) => {
    const request = input instanceof Request ? input : new Request(input, init)
    const url = new URL(request.url)
    return `${request.method} ${url.pathname}${url.search}`
  })
}
