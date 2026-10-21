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

// Each test starts with an empty DOM and an empty RTK Query cache.
afterEach(() => {
  cleanup()
  store.dispatch(api.util.resetApiState())
})
