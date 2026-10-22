// Manat with at most two decimals; a comma is accepted as the decimal mark.
const PRICE = /^\d{1,8}([.,]\d{1,2})?$/

/** "8,5" -> 8.5; null when it is not a valid price. */
export function parsePrice(text: string): number | null {
  const value = text.trim()
  return PRICE.test(value) ? Number(value.replace(',', '.')) : null
}
