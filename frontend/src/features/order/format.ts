// Currency formatting for bill amounts stored in minor units (qəpik).

import { currentLocale } from '@/i18n'

/**
 * Format a minor-unit amount (e.g. 1250 → "12,50 ₼") for display.
 *
 * @param minorAmount Amount in minor currency units.
 * @param currency    ISO-4217 code, e.g. "AZN".
 */
export function formatCurrency(minorAmount: number, currency: string): string {
  return new Intl.NumberFormat(currentLocale(), {
    style: 'currency',
    currency,
  }).format(minorAmount / 100)
}
