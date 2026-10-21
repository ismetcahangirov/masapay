import { api } from '@/app/api'
import type {
  Order,
  OrderItem,
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
}

interface EqualSplitDto {
  totalParts: number
  paidParts: number
}

interface OrderDto {
  orderId: string
  status: 'OPEN' | 'CLOSED' | 'PAID' | 'CANCELLED'
  totalAmount: number
  items: OrderItemDto[]
  equalSplit: EqualSplitDto | null
}

export interface TableOrderDto {
  tableId: string
  tableLabel: string
  restaurantId: string
  restaurantName: string
  currency: string
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
    paid: item.paid,
  }))
  const totalAmount = toMinor(dto.totalAmount)

  let paidAmount: number
  if (dto.status === 'PAID') {
    paidAmount = totalAmount
  } else if (dto.equalSplit && dto.equalSplit.totalParts > 0) {
    paidAmount = Math.round(
      (totalAmount * dto.equalSplit.paidParts) / dto.equalSplit.totalParts,
    )
  } else {
    paidAmount = items
      .filter((item) => item.paid)
      .reduce((sum, item) => sum + item.total, 0)
  }

  let status: OrderStatus
  if (dto.status === 'PAID') status = 'paid'
  else if (paidAmount > 0) status = 'partially_paid'
  else if (dto.status === 'CLOSED') status = 'awaiting_payment'
  else status = 'open'

  return { id: dto.orderId, status, items, totalAmount, paidAmount, currency }
}

/** Maps the backend response into the UI's TableOrder. */
export function toTableOrder(dto: TableOrderDto): TableOrder {
  return {
    tableId: dto.tableId,
    tableLabel: dto.tableLabel,
    restaurantName: dto.restaurantName,
    currency: dto.currency,
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
    }),
  }),
})

export const { useGetTableOrderQuery } = orderApi
