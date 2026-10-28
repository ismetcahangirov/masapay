import i18n from '@/i18n'
import type { AvailabilityMode, MenuDish } from './menu-api'

// Daily menu (#139) helpers: which mode a dish is in and how to describe it.

/** "2026-10-24" -> "24.10" */
export function shortDate(iso: string): string {
  const [, month, day] = iso.split('-')
  return `${day}.${month}`
}

/** The mode the availability dialog starts in for this dish. */
export function currentMode(dish: MenuDish): AvailabilityMode {
  if (!dish.active) return 'OFF'
  if (dish.availableFrom || dish.availableUntil) return 'DATES'
  if (dish.offOn && !dish.availableToday) return 'OFF_TODAY'
  return 'ALWAYS'
}

/** A short status for the menu row; null when the dish is simply on the menu. */
export function availabilityLabel(dish: MenuDish): string | null {
  if (!dish.active) return i18n.t('menu:availability.off')
  if (dish.availableFrom || dish.availableUntil) {
    const from = dish.availableFrom ? shortDate(dish.availableFrom) : '...'
    const until = dish.availableUntil ? shortDate(dish.availableUntil) : '...'
    return `${from} - ${until}`
  }
  if (dish.availableToday) return null
  if (dish.offOn) return i18n.t('menu:availability.offToday')
  // Only a combo can be off without a setting of its own: a dish inside is off.
  return i18n.t('menu:availability.comboPartOff')
}
