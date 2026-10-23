import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'
import { store } from '@/app/store'
import { api } from '@/app/api'

// jsdom provides its own AbortSignal, which Node's built-in Request rejects
// ("Expected signal to be an instance of AbortSignal"). RTK Query passes one to
// every request, so in tests the signal is dropped; nothing here aborts fetches.
const NodeRequest = globalThis.Request
globalThis.Request = class extends NodeRequest {
  constructor(input: RequestInfo | URL, init?: RequestInit) {
    super(input, init ? { ...init, signal: undefined } : init)
  }
} as typeof Request

// Newer Node versions ship their own global localStorage that shadows jsdom's
// and has no working methods unless Node is started with a storage file. Give
// tests a plain in-memory Storage so code that remembers things per browser
// (the customer basket, #140) behaves as in a real browser.
if (typeof window.localStorage?.clear !== 'function') {
  const data = new Map<string, string>()
  const memoryStorage: Storage = {
    get length() {
      return data.size
    },
    clear: () => data.clear(),
    getItem: (key) => data.get(key) ?? null,
    key: (index) => [...data.keys()][index] ?? null,
    removeItem: (key) => void data.delete(key),
    setItem: (key, value) => void data.set(key, String(value)),
  }
  Object.defineProperty(window, 'localStorage', {
    configurable: true,
    value: memoryStorage,
  })
}

// Each test starts with an empty DOM and an empty RTK Query cache.
afterEach(() => {
  cleanup()
  store.dispatch(api.util.resetApiState())
})

// jsdom has no matchMedia; the admin sidebar (#184) asks it whether the screen
// is a phone. Tests run as a desktop: no media query matches.
if (typeof window.matchMedia !== 'function') {
  window.matchMedia = (query: string) =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }) as MediaQueryList
}
