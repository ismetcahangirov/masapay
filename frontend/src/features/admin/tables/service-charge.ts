import i18n from '@/i18n'
import type { AdminTable } from './tables-api'

// Per-table service charge (#146) helpers.

/** "15" or "12,5" -> 15 / 12.5 (0 < x <= 100, two decimals); null otherwise. */
export function parsePercent(text: string): number | null {
  const value = text.trim()
  if (!/^\d{1,3}([.,]\d{1,2})?$/.test(value)) return null
  const percent = Number(value.replace(',', '.'))
  return percent > 0 && percent <= 100 ? percent : null
}

/** What the table card says: "15% (bu masa)", "10% (restoran)" or "Yoxdur". */
export function serviceChargeLabel(table: AdminTable): string {
  if (table.effectiveServiceChargePercent === null)
    return i18n.t('tables:serviceCharge.none')
  const percent = `${table.effectiveServiceChargePercent}%`
  return table.serviceChargePercent === null
    ? i18n.t('tables:serviceCharge.restaurant', { percent })
    : i18n.t('tables:serviceCharge.thisTable', { percent })
}
