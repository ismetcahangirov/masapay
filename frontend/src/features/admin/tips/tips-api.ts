import { api } from '@/app/api'
import { toMinor } from '@/features/order/order-api'
import i18n from '@/i18n'
import { asDownload, type DownloadedFile } from '@/lib/download'

// Per-waiter tip report of a restaurant (#48). Managers and super admins read
// it for a date range (inclusive ISO dates, Asia/Baku) and export it as CSV or
// as a formatted Excel workbook (#170).

export interface TipRange {
  restaurantId: string
  /** ISO date, e.g. "2026-10-01". */
  from: string
  /** ISO date, inclusive. */
  to: string
}

interface WaiterTipsDto {
  waiterId: string | null
  waiterName: string | null
  waiterEmail: string | null
  tipCount: number
  tipTotal: number
}

interface TipReportDto {
  restaurantId: string
  currency: string
  from: string
  to: string
  timeZone: string
  waiters: WaiterTipsDto[]
  totalTipCount: number
  totalTipAmount: number
}

export interface WaiterTips {
  /** null for tips on orders with no waiter assigned. */
  waiterId: string | null
  waiterName: string | null
  waiterEmail: string | null
  tipCount: number
  /** Minor units (qəpik). */
  tipTotal: number
}

export interface TipReport {
  restaurantId: string
  currency: string
  from: string
  to: string
  timeZone: string
  /** Sorted by total, highest first. */
  waiters: WaiterTips[]
  totalTipCount: number
  /** Minor units (qəpik). */
  totalTipAmount: number
}

function toReport(dto: TipReportDto): TipReport {
  return {
    ...dto,
    waiters: dto.waiters
      .map((waiter) => ({ ...waiter, tipTotal: toMinor(waiter.tipTotal) }))
      .sort((a, b) => b.tipTotal - a.tipTotal),
    totalTipAmount: toMinor(dto.totalTipAmount),
  }
}

export const tipsApi = api.injectEndpoints({
  endpoints: (build) => ({
    tipReport: build.query<TipReport, TipRange>({
      query: ({ restaurantId, from, to }) => ({
        url: `/admin/restaurants/${restaurantId}/tips`,
        params: { from, to },
      }),
      transformResponse: toReport,
    }),
    /** The same report as a CSV file. */
    tipReportCsv: build.mutation<Blob, TipRange>({
      query: ({ restaurantId, from, to }) => ({
        url: `/admin/restaurants/${restaurantId}/tips/export`,
        params: { from, to },
        responseHandler: (response) => response.blob(),
      }),
    }),
    /** The same report as a formatted Excel workbook (#170). */
    tipReportExcel: build.mutation<DownloadedFile, TipRange>({
      query: ({ restaurantId, from, to }) => ({
        url: `/admin/restaurants/${restaurantId}/tips/export/xlsx`,
        params: { from, to },
        responseHandler: asDownload(
          `${i18n.t('tips:files.report', { from, to })}.xlsx`,
        ),
      }),
    }),
  }),
})

export const {
  useTipReportQuery,
  useTipReportCsvMutation,
  useTipReportExcelMutation,
} = tipsApi
