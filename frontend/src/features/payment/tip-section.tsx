import { HandCoins } from 'lucide-react'
import { formatCurrency } from '@/features/order/format'
import { TipSelector } from '@/features/tip/tip-selector'

// The tip on a payment (#181): ready percentages or "Başqa məbləğ" for an
// amount of one's own, then the total charged. A tip larger than the bill part
// is refused here as well as by the backend (a slip like 50 instead of 5).
export function TipSection({
  amountMinor,
  currency,
  tipMinor,
  onTipChange,
}: {
  /** The bill part being paid, in minor units; the percentages apply to it. */
  amountMinor: number
  currency: string
  tipMinor: number
  onTipChange: (tipMinor: number) => void
}) {
  const tooBig = tipMinor > amountMinor
  return (
    <section aria-label="Bəxşiş" className="space-y-2 rounded-lg border p-3">
      <h3 className="flex items-center gap-2 text-sm font-semibold">
        <HandCoins className="size-4" aria-hidden />
        Bəxşiş
      </h3>
      <TipSelector
        baseMinor={amountMinor}
        currency={currency}
        onChange={onTipChange}
      />
      {tooBig && (
        <p className="text-sm text-destructive">
          Bəxşiş ödənilən hissədən çox ola bilməz.
        </p>
      )}
      <p className="flex justify-between border-t pt-2 text-sm font-medium">
        <span>Cəmi ödəniş</span>
        <span className="tabular-nums">
          {formatCurrency(amountMinor + tipMinor, currency)}
        </span>
      </p>
    </section>
  )
}
