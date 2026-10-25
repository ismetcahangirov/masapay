#!/usr/bin/env node
// End-to-end payment tests against the Payriff emulator
// (https://github.com/ismetcahangirov/payriff-emulator).
//
// A real masapay backend (not the mock) talks to the emulator over HTTP, and
// every scenario goes through the public API the customer's phone uses, the
// emulator's control API (pay, age an order, inject a fault, resend a callback)
// and the admin API. Each scenario opens its own new table, so demo data is
// not touched.
//
// Start the emulator (fast callback retries for tests), then the backend:
//   PORT=4010 CALLBACK_FIRST_RETRY_MS=2000 CALLBACK_RETRY_MS=2000 node dist/server.js
//   MASAPAY_PAYRIFF_MOCK=false MASAPAY_PAYRIFF_BASE_URL=http://localhost:4010 \
//   MASAPAY_PAYRIFF_APP_KEY=emulator-secret-key MASAPAY_DEV_LOGIN_ENABLED=true \
//   MASAPAY_PAYRIFF_CALLBACK_URL=http://localhost:8081/api/public/payriff/webhook \
//   MASAPAY_RATE_LIMIT_PAYMENT_PER_MINUTE=1000 SERVER_PORT=8081 ./gradlew bootRun
// Then:
//   node scripts/e2e/payriff-emulator.mjs
//
// Environment: BACKEND (http://localhost:8081), EMULATOR (http://localhost:4010),
// RESTAURANT_ID (the seed's Bulvar Cafe), ADMIN_EMAIL (admin@masapay.local).
// Exits with code 1 when any check fails.

const BACKEND = process.env.BACKEND ?? 'http://localhost:8081'
const EMULATOR = process.env.EMULATOR ?? 'http://localhost:4010'
const RESTAURANT_ID = process.env.RESTAURANT_ID ?? 'a0000000-0000-0000-0000-000000000002'
const ADMIN_EMAIL = process.env.ADMIN_EMAIL ?? 'admin@masapay.local'

const CARD = {
  approved: '4111111111111111',
  declined: '4000000000000002',
  insufficient: '4000000000009995',
}

let failures = 0
let token = null

function check(condition, label, detail = '') {
  if (condition) {
    console.log(`    ok    ${label}`)
  } else {
    failures++
    console.log(`    FAIL  ${label}${detail ? `\n          ${detail}` : ''}`)
  }
}

async function http(method, url, { body, auth = false, redirect = 'follow' } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) headers.Authorization = `Bearer ${token}`
  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    redirect,
  })
  const text = await response.text()
  let json = null
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    json = null
  }
  return { status: response.status, json, text, headers: response.headers }
}

const backend = (method, path, options) => http(method, BACKEND + path, options)
const emulator = (method, path, options) => http(method, EMULATOR + path, options)

async function waitFor(read, done, { timeoutMs = 8000 } = {}) {
  const until = Date.now() + timeoutMs
  let last
  while (Date.now() < until) {
    last = await read()
    if (done(last)) return last
    await new Promise((r) => setTimeout(r, 250))
  }
  return last
}

/** A new table in the test restaurant with one line of `amount` manat on its bill. */
async function newBill(amount) {
  const label = `E2E ${new Date().toISOString().slice(11, 19)} ${Math.random().toString(36).slice(2, 6)}`
  const table = await backend('POST', `/api/admin/restaurants/${RESTAURANT_ID}/tables`, {
    auth: true,
    body: { label },
  })
  if (table.status !== 201) throw new Error(`table: ${table.status} ${table.text}`)
  const line = await backend('POST', `/api/admin/tables/${table.json.id}/order/items`, {
    auth: true,
    body: { name: 'E2E kabab', quantity: 1, unitPrice: amount },
  })
  if (line.status !== 200) throw new Error(`line: ${line.status} ${line.text}`)
  return { tableId: table.json.id, qr: table.json.qrToken, label }
}

async function checkoutFull(qr, tipAmount = null) {
  const r = await backend('POST', `/api/public/tables/${qr}/payment/full/checkout`, {
    body: { tipAmount },
  })
  const orderId = r.json?.paymentUrl?.split('/pay/')[1]
  return { ...r, transactionId: r.json?.transactionId, orderId }
}

const paymentStatus = (transactionId) => backend('GET', `/api/public/payments/${transactionId}`)
const emulatorOrder = (orderId) => emulator('GET', `/_emulator/orders/${orderId}`)

async function payOnEmulator(orderId, pan) {
  return emulator('POST', `/_emulator/orders/${orderId}/pay`, { body: { pan } })
}

async function paidBill(amount, tip = null) {
  const { qr } = await newBill(amount)
  const checkout = await checkoutFull(qr, tip)
  await payOnEmulator(checkout.orderId, CARD.approved)
  const status = await waitFor(
    () => paymentStatus(checkout.transactionId),
    (r) => r.json?.status !== 'PENDING',
  )
  return { qr, checkout, status }
}

async function scenario(name, run) {
  console.log(`\n${name}`)
  try {
    await run()
  } catch (error) {
    failures++
    console.log(`    FAIL  threw: ${error.message}`)
  }
}

// ---------------------------------------------------------------------------

const health = await emulator('GET', '/health')
if (health.status !== 200) {
  console.error(`The emulator is not reachable at ${EMULATOR}`)
  process.exit(2)
}
await emulator('POST', '/_emulator/reset')
await emulator('DELETE', '/_emulator/faults')
const login = await backend('POST', '/api/auth/dev-login', { body: { email: ADMIN_EMAIL } })
if (login.status !== 200) {
  console.error(`Dev login failed (${login.status}); start the backend with MASAPAY_DEV_LOGIN_ENABLED=true`)
  process.exit(2)
}
token = login.json.tokens.accessToken

await scenario('1. Approved card: the bill and the tip are charged, the callback settles it', async () => {
  const { qr } = await newBill(20)
  const checkout = await checkoutFull(qr, 2.5)
  check(checkout.status === 200 && checkout.orderId, 'checkout opens a Payriff order', `${checkout.status} ${checkout.text}`)
  const order = await emulatorOrder(checkout.orderId)
  // Read from the raw JSON: JavaScript would turn 22.50 into 22.5.
  check(order.text.includes('"amount":22.50,'), 'Payriff charges bill + tip exactly (22.50)', order.text.slice(0, 200))
  await payOnEmulator(checkout.orderId, CARD.approved)
  const status = await waitFor(() => paymentStatus(checkout.transactionId), (r) => r.json?.status !== 'PENDING')
  check(status.json?.status === 'SUCCESS', 'the payment is SUCCESS after the callback', `got ${status.json?.status}`)
  check(status.json?.orderStatus === 'PAID', 'the bill is PAID', `got ${status.json?.orderStatus}`)
  const after = await emulatorOrder(checkout.orderId)
  const attempts = after.json?.payload?.callbacks?.flatMap((c) => c.attempts ?? []) ?? []
  check(attempts.some((a) => a.httpStatus === 200), 'our webhook answered the callback with 200', JSON.stringify(attempts))
})

await scenario('2. After paying, the browser comes back through the callback URL (GET)', async () => {
  const { checkout } = await paidBill(10)
  const order = await emulatorOrder(checkout.orderId)
  const callbackUrl = order.json?.payload?.callbackUrl
  const back = await http('GET', callbackUrl, { redirect: 'manual' })
  const location = back.headers.get('location') ?? ''
  check(back.status >= 300 && back.status < 400, 'the GET answers a redirect, not an error', `got ${back.status} ${back.text.slice(0, 120)}`)
  check(location.includes('/payment/result') && location.includes(`tx=${checkout.transactionId}`), 'it leads to our result page for this payment', `location: ${location}`)
})

await scenario('3. Declined card: the payment fails, the bill stays open', async () => {
  const { qr } = await newBill(15)
  const checkout = await checkoutFull(qr)
  await payOnEmulator(checkout.orderId, CARD.declined)
  const status = await waitFor(() => paymentStatus(checkout.transactionId), (r) => r.json?.status !== 'PENDING')
  check(status.json?.status === 'FAILED', 'the payment is FAILED', `got ${status.json?.status}`)
  check(status.json?.orderStatus === 'OPEN', 'the bill is still OPEN to pay again', `got ${status.json?.orderStatus}`)
  const again = await checkoutFull(qr)
  check(again.status === 200, 'the guest can start a new payment', `${again.status} ${again.text}`)
})

await scenario('4. Insufficient funds: the payment fails', async () => {
  const { qr } = await newBill(15)
  const checkout = await checkoutFull(qr)
  await payOnEmulator(checkout.orderId, CARD.insufficient)
  const status = await waitFor(() => paymentStatus(checkout.transactionId), (r) => r.json?.status !== 'PENDING')
  check(status.json?.status === 'FAILED', 'the payment is FAILED', `got ${status.json?.status}`)
})

await scenario('5. The same callback twice (Payriff may resend): settled once', async () => {
  const { qr, checkout } = await paidBill(12)
  const resent = await emulator('POST', `/_emulator/orders/${checkout.orderId}/callback`)
  check(resent.status === 200, 'the emulator resent the callback', `${resent.status} ${resent.text}`)
  await new Promise((r) => setTimeout(r, 800))
  const bill = await backend('GET', `/api/public/tables/${qr}/order`)
  check(Number(bill.json?.order?.paidAmount) === 12, 'the bill is paid once, not twice (12.00)', `paid ${bill.json?.order?.paidAmount}`)
})

await scenario('6. A forged callback (bad signature) is refused', async () => {
  const { qr } = await newBill(9)
  const checkout = await checkoutFull(qr)
  const forged = await backend('POST', `/api/public/payriff/webhook?tx=${checkout.transactionId}&sig=forged`, {
    body: { payload: { orderId: checkout.orderId, paymentStatus: 'APPROVED' } },
  })
  check(forged.status === 400 || forged.status === 401 || forged.status === 403, 'answered 4xx', `got ${forged.status}`)
  const status = await paymentStatus(checkout.transactionId)
  check(status.json?.status === 'PENDING', 'the payment did not become SUCCESS', `got ${status.json?.status}`)
})

await scenario('7. Refund within 30 days', async () => {
  const { checkout } = await paidBill(18)
  const refund = await backend('POST', `/api/admin/transactions/${checkout.transactionId}/refund`, {
    auth: true,
    body: { reason: 'E2E' },
  })
  check(refund.status === 200 && refund.json?.status === 'REFUNDED', 'masapay marks it REFUNDED', `${refund.status} ${refund.text}`)
  const order = await emulator('GET', `/api/v3/orders/${checkout.orderId}`, {})
  const info = await fetch(`${EMULATOR}/api/v3/orders/${checkout.orderId}`, {
    headers: { Authorization: 'emulator-secret-key' },
  }).then((r) => r.json())
  check(info?.payload?.paymentStatus === 'REFUNDED', 'Payriff has the order REFUNDED', `got ${info?.payload?.paymentStatus} (${order.status})`)
})

await scenario('8. Refund after 30 days: Payriff refuses, masapay says why and keeps the payment', async () => {
  const { checkout } = await paidBill(18)
  await emulator('POST', `/_emulator/orders/${checkout.orderId}/age`, { body: { days: 31 } })
  const refund = await backend('POST', `/api/admin/transactions/${checkout.transactionId}/refund`, {
    auth: true,
    body: { reason: 'E2E late' },
  })
  check(refund.status >= 400 && refund.status < 500, 'answered 4xx, not a server error', `got ${refund.status} ${refund.text.slice(0, 160)}`)
  check(/30|expired|müddət/i.test(refund.text), 'the answer explains the 30-day window', refund.text.slice(0, 160))
  const status = await paymentStatus(checkout.transactionId)
  check(status.json?.status === 'SUCCESS', 'the payment is still SUCCESS', `got ${status.json?.status}`)
})

await scenario('9. Payriff is down (502) when the guest starts paying', async () => {
  const { qr, label } = await newBill(11)
  await emulator('POST', '/_emulator/faults', {
    body: { method: 'POST', path: '/api/v3/orders', status: 502, raw: '<html>Bad Gateway</html>', times: 1 },
  })
  const failed = await checkoutFull(qr)
  check(failed.status === 502 || failed.status === 503, 'answered 502/503, not 500', `got ${failed.status} ${failed.text.slice(0, 120)}`)
  const list = await backend('GET', `/api/admin/restaurants/${RESTAURANT_ID}/transactions?size=100`, {
    auth: true,
  })
  const mine = (list.json?.items ?? []).filter((t) => t.tableLabel === label)
  check(
    mine.length === 1 && mine[0].status === 'FAILED',
    'the attempt is recorded as FAILED, nothing is left PENDING',
    JSON.stringify(mine.map((t) => t.status)),
  )
  const again = await checkoutFull(qr)
  check(again.status === 200, 'once Payriff is back, paying works', `${again.status} ${again.text}`)
})

await scenario('10. Payriff answers HTTP 200 with an error code', async () => {
  const { qr } = await newBill(11)
  await emulator('POST', '/_emulator/faults', {
    body: { method: 'POST', path: '/api/v3/orders', status: 200, code: '15000', message: 'Internal Error', times: 1 },
  })
  const failed = await checkoutFull(qr)
  check(failed.status === 502 || failed.status === 503, 'answered 502/503, not 500', `got ${failed.status} ${failed.text.slice(0, 120)}`)
})

await scenario('11. Equal split with a tip: exact decimals reach Payriff', async () => {
  const { qr } = await newBill(10)
  // The first of three parts carries the leftover cent (3.34), so the quote decides.
  const quote = await backend('GET', `/api/public/tables/${qr}/payment/equal?parts=3&partsToPay=1`)
  const expected = (Math.round(quote.json.amount * 100) + 67) / 100
  const r = await backend('POST', `/api/public/tables/${qr}/payment/equal/checkout`, {
    body: { parts: 3, partsToPay: 1, tipAmount: 0.67 },
  })
  const orderId = r.json?.paymentUrl?.split('/pay/')[1]
  check(r.status === 200, 'checkout opens', `${r.status} ${r.text}`)
  const order = await emulatorOrder(orderId)
  check(
    order.text.includes(`"amount":${expected.toFixed(2)},`),
    `the share (${quote.json.amount}) + 0.67 tip = ${expected.toFixed(2)}, exact`,
    order.text.slice(0, 200),
  )
})

await scenario('12. Each create request carries a UUID X-REQUEST-RRN (idempotency)', async () => {
  const { qr } = await newBill(7)
  const checkout = await checkoutFull(qr)
  const order = await emulatorOrder(checkout.orderId)
  const rrn = order.json?.payload?.requestRrn
  check(rrn === checkout.transactionId, 'the RRN is our transaction id, so a retry finds the same order', `rrn ${rrn}`)
})

console.log(failures === 0 ? '\nAll checks passed.' : `\n${failures} check(s) failed.`)
process.exit(failures === 0 ? 0 : 1)
