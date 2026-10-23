import { api } from '@/app/api'
import { toMinor } from '@/features/order/order-api'

// A waiter's own tips with the table of each (#182). Amounts in minor units.

interface MyTipsDto {
  from: string
  to: string
  timeZone: string
  currency: string
  tipCount: number
  tipTotal: number
  tips: {
    transactionId: string
    paidAt: string
    tableLabel: string
    tipAmount: number
    billAmount: number
  }[]
}

export interface MyTip {
  id: string
  /** ISO time. */
  paidAt: string
  tableLabel: string
  tipMinor: number
  billMinor: number
}

export interface MyTips {
  from: string
  to: string
  currency: string
  tipCount: number
  tipTotalMinor: number
  /** Newest first. */
  tips: MyTip[]
}

export const myTipsApi = api.injectEndpoints({
  endpoints: (build) => ({
    myTips: build.query<MyTips, { from: string; to: string }>({
      query: (range) => ({ url: '/admin/me/tips', params: range }),
      transformResponse: (dto: MyTipsDto): MyTips => ({
        from: dto.from,
        to: dto.to,
        currency: dto.currency,
        tipCount: dto.tipCount,
        tipTotalMinor: toMinor(dto.tipTotal),
        tips: dto.tips.map((tip) => ({
          id: tip.transactionId,
          paidAt: tip.paidAt,
          tableLabel: tip.tableLabel,
          tipMinor: toMinor(tip.tipAmount),
          billMinor: toMinor(tip.billAmount),
        })),
      }),
    }),
  }),
})

export const { useMyTipsQuery } = myTipsApi
