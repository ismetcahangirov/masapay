import { api } from '@/app/api'
import { subscribeToTopic } from '@/lib/stomp'
import type {
  Order,
  OrderItem,
  KitchenStatus,
  LineSource,
  OrderStatus,
  TableOrder,
} from '@/features/order/types'

// JSON shapes of GET /api/public/tables/{qrToken}/order. Money arrives as
// decimal numbers (e.g. 12.5) and is converted to minor units (1250) once, here.

interface OrderItemDto {
  id: string
  name: string
  quantity: number
  unitPrice: number
  totalPrice: number
  paid: boolean
  kitchenStatus?: KitchenStatus | null
  source?: LineSource
  addedBy?: string | null
  addedAt?: string
}

interface EqualSplitDto {
  totalParts: number
  paidParts: number
}

interface OrderDto {
  orderId: string
  status: 'OPEN' | 'CLOSED' | 'PAID' | 'CANCELLED'
  totalAmount: number
  /** What successful payments have covered so far (tips excluded). */
  paidAmount: number
  outstanding: number
  items: OrderItemDto[]
  equalSplit: EqualSplitDto | null
  serviceCharge: { amount: number; guests: number | null } | null
  /** Staff views only (#182); never sent to customers. */
  waiter?: { id: string; name: string } | null
}

export interface TableOrderDto {
  tableId: string
  tableLabel: string
  restaurantId: string
  restaurantName: string
  currency: string
  /** Restaurant service charge in percent, null when it charges none. */
  serviceChargePercent: number | null
  order: OrderDto | null
}

/** Decimal amount (12.5) to minor units (1250), rounded to avoid float noise. */
export function toMinor(amount: number): number {
  return Math.round(amount * 100)
}

function toOrder(dto: OrderDto, currency: string): Order {
  const items: OrderItem[] = dto.items.map((item) => ({
    id: item.id,
    name: item.name,
    quantity: item.quantity,
    unitPrice: toMinor(item.unitPrice),
    total: toMinor(item.totalPrice),
    // A fully paid bill shows every line as paid (crossed out on the receipt).
    paid: item.paid || dto.status === 'PAID',
    kitchenStatus: item.kitchenStatus ?? null,
    source: item.source ?? 'STAFF',
    addedBy: item.addedBy ?? null,
    addedAt: item.addedAt ?? null,
  }))
  const totalAmount = toMinor(dto.totalAmount)

  // The backend sums the successful payments, so paid vs. remaining is exact even
  // when item payments and an equal split were mixed on one bill.
  const paidAmount =
    dto.status === 'PAID' ? totalAmount : toMinor(dto.paidAmount)

  let status: OrderStatus
  if (dto.status === 'PAID') status = 'paid'
  else if (dto.status === 'CANCELLED') status = 'cancelled'
  else if (paidAmount > 0) status = 'partially_paid'
  else if (dto.status === 'CLOSED') status = 'awaiting_payment'
  else status = 'open'

  return {
    id: dto.orderId,
    status,
    items,
    totalAmount,
    paidAmount,
    currency,
    equalSplit: dto.equalSplit,
    serviceCharge: dto.serviceCharge
      ? {
          amount: toMinor(dto.serviceCharge.amount),
          guests: dto.serviceCharge.guests,
        }
      : null,
    waiter: dto.waiter ?? null,
  }
}

/** Maps the backend response into the UI's TableOrder. */
export function toTableOrder(dto: TableOrderDto): TableOrder {
  return {
    tableId: dto.tableId,
    tableLabel: dto.tableLabel,
    restaurantName: dto.restaurantName,
    currency: dto.currency,
    serviceChargePercent: dto.serviceChargePercent,
    order: dto.order ? toOrder(dto.order, dto.currency) : null,
  }
}

export const orderApi = api.injectEndpoints({
  endpoints: (build) => ({
    /** The live bill for the table behind a QR code's token. */
    getTableOrder: build.query<TableOrder, string>({
      query: (qrToken) => `/public/tables/${encodeURIComponent(qrToken)}/order`,
      transformResponse: (dto: TableOrderDto) => toTableOrder(dto),
      providesTags: (_result, _error, qrToken) => [
        { type: 'TableOrder', id: qrToken },
      ],
      // Live updates (#21): while a screen shows this bill, keep a STOMP
      // subscription to /topic/tables/{qrToken}. The backend pushes the whole
      // fresh bill when the POS adds an item or someone pays, and it replaces the
      // cached one, so the screen updates without a manual refresh.
      async onCacheEntryAdded(
        qrToken,
        { updateCachedData, cacheDataLoaded, cacheEntryRemoved, dispatch },
      ) {
        try {
          await cacheDataLoaded
        } catch {
          return // the first load failed; nothing to keep live
        }
        let connectedBefore = false
        const subscription = subscribeToTopic(
          `/topic/tables/${qrToken}`,
          (body) => {
            try {
              const next = toTableOrder(JSON.parse(body) as TableOrderDto)
              updateCachedData(() => next)
            } catch {
              // Ignore a malformed push; the next one or a refetch fixes it.
            }
          },
          () => {
            // After a reconnect, refetch once to pick up anything missed.
            if (connectedBefore) {
              dispatch(
                orderApi.util.invalidateTags([
                  { type: 'TableOrder', id: qrToken },
                ]),
              )
            }
            connectedBefore = true
          },
        )
        await cacheEntryRemoved
        subscription.close()
      },
    }),
  }),
})

export const { useGetTableOrderQuery } = orderApi
