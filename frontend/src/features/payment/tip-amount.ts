/** The tip as the backend takes it (#181): manat with two decimals, or none. */
export function tipAmount(tipMinor: number): number | null {
  return tipMinor > 0 ? tipMinor / 100 : null
}
