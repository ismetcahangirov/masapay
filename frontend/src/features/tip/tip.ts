// Tip calculation helpers. Amounts are in minor currency units (qəpik).

/** Preset tip percentages offered to the guest. */
export const TIP_PERCENT_PRESETS = [0, 5, 10, 15] as const

export type TipPercentPreset = (typeof TIP_PERCENT_PRESETS)[number]

/** Tip amount (minor units) for a percentage of the base bill, rounded. */
export function tipFromPercent(baseMinor: number, percent: number): number {
  return Math.round((baseMinor * percent) / 100)
}

/**
 * Parse a custom tip typed in major currency units (AZN) into minor units.
 *
 * Accepts digits with an optional decimal separator (comma or dot) and at most
 * two fractional digits. Returns `null` for anything invalid — empty input,
 * negative values, non-numeric characters or too many decimals — so the caller
 * can surface a validation error and never emit a bogus amount.
 */
export function parseTipInput(raw: string): number | null {
  const trimmed = raw.trim()
  if (trimmed === '') return null

  // Digits, one optional separator, up to two decimals. A leading "-" is not
  // part of the pattern, so negative input is rejected outright.
  const match = /^(\d+)(?:[.,](\d{1,2}))?$/.exec(trimmed)
  if (!match) return null

  const whole = Number.parseInt(match[1], 10)
  const fraction = match[2] ? match[2].padEnd(2, '0') : '00'
  return whole * 100 + Number.parseInt(fraction, 10)
}
