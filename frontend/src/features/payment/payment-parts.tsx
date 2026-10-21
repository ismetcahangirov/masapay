import type { ReactNode } from 'react'
import { CircleAlert, CircleCheckBig, Minus, Plus } from 'lucide-react'
import { formatCurrency } from '@/features/order/format'
import type { Receipt } from '@/features/payment/payment-api'
import { Button } from '@/components/ui/button'

// Small building blocks shared by the three payment dialogs.

/** "- 2 +" counter, used for the number of people and shares. */
export function Stepper({
  label,
  value,
  min,
  max,
  onChange,
  disabled = false,
}: {
  label: string
  value: number
  min: number
  max: number
  onChange: (value: number) => void
  disabled?: boolean
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-sm">{label}</span>
      <div className="flex items-center gap-3">
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label={`${label}: azalt`}
          disabled={disabled || value <= min}
          onClick={() => onChange(value - 1)}
        >
          <Minus aria-hidden />
        </Button>
        <span
          className="w-8 text-center text-lg font-semibold tabular-nums"
          aria-live="polite"
        >
          {value}
        </span>
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label={`${label}: artır`}
          disabled={disabled || value >= max}
          onClick={() => onChange(value + 1)}
        >
          <Plus aria-hidden />
        </Button>
      </div>
    </div>
  )
}

/** The amount the payer is about to pay, or a placeholder while it loads. */
export function AmountDue({
  amount,
  currency,
  loading,
}: {
  amount: number | undefined
  currency: string
  loading: boolean
}) {
  return (
    <div className="flex items-center justify-between rounded-lg bg-muted px-4 py-3">
      <span className="text-sm text-muted-foreground">Ödəniləcək</span>
      {loading || amount === undefined ? (
        <span className="h-6 w-20 animate-pulse rounded bg-background" />
      ) : (
        <span className="text-lg font-semibold tabular-nums">
          {formatCurrency(amount, currency)}
        </span>
      )}
    </div>
  )
}

export function ErrorNote({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="flex items-start gap-2 text-sm text-destructive">
      <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
      {children}
    </p>
  )
}

/** Shown in place of the form once the payment went through. */
export function PaymentSuccess({
  receipt,
  onClose,
}: {
  receipt: Receipt
  onClose: () => void
}) {
  const settled = receipt.orderStatus === 'PAID'
  return (
    <div className="flex flex-col items-center gap-4 py-2 text-center">
      <div className="flex size-14 items-center justify-center rounded-full bg-success/15 text-success">
        <CircleCheckBig className="size-7" aria-hidden />
      </div>
      <div className="space-y-1">
        <p className="text-lg font-semibold">Ödəniş qəbul edildi</p>
        <p className="text-sm text-muted-foreground">
          {formatCurrency(receipt.amount, receipt.currency)}
          {settled ? ' - hesab tam bağlandı.' : ' - hesabın qalığı yeniləndi.'}
        </p>
      </div>
      <Button className="w-full" onClick={onClose}>
        Bağla
      </Button>
    </div>
  )
}
