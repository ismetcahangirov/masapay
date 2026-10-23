import { useState } from 'react'
import { Check } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { formatCurrency } from '@/features/order/format'
import type { Order } from '@/features/order/types'
import {
  useCheckoutItemsMutation,
  useQuoteItemsQuery,
  goToPayment,
} from '@/features/payment/payment-api'
import { AmountDue, ErrorNote, Stepper } from '@/features/payment/payment-parts'
import { paymentErrorMessage } from '@/features/payment/payment-errors'
import { tipAmount } from '@/features/payment/tip-amount'
import { TipSection } from '@/features/payment/tip-section'
import { cn } from '@/lib/utils'

const MAX_GUESTS = 50

// "Öz yediyini ödə" (#25): the guest ticks the lines they ate and pays only
// those. Lines already paid by someone else are shown but cannot be picked.
// If the bill has a service charge that nobody has split yet, the first payer
// also says how many guests share it.
export function ItemPaymentDialog({
  qrToken,
  order,
  open,
  onOpenChange,
}: {
  qrToken: string
  order: Order
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const [selected, setSelected] = useState<string[]>([])
  const [guests, setGuests] = useState(2)

  const needsGuests =
    !!order.serviceCharge && order.serviceCharge.guests === null
  const guestCount = needsGuests ? guests : undefined
  const unpaidIds = new Set(
    order.items.filter((item) => !item.paid).map((item) => item.id),
  )
  // Drop lines someone else paid while this dialog was open.
  const chosen = selected.filter((id) => unpaidIds.has(id))

  const quote = useQuoteItemsQuery(
    { qrToken, itemIds: chosen, guests: guestCount },
    { skip: !open || chosen.length === 0 },
  )
  const [pay, payState] = useCheckoutItemsMutation()
  const [tipMinor, setTipMinor] = useState(0)

  function toggle(id: string) {
    setSelected((current) =>
      current.includes(id)
        ? current.filter((value) => value !== id)
        : [...current, id],
    )
  }

  function close(next: boolean) {
    if (!next) {
      setSelected([])
      setTipMinor(0)
      payState.reset()
    }
    onOpenChange(next)
  }

  async function onPay() {
    try {
      goToPayment(
        await pay({
          qrToken,
          itemIds: chosen,
          guests: guestCount,
          tipAmount: tipAmount(tipMinor),
        }).unwrap(),
      )
    } catch {
      if (chosen.length > 0) void quote.refetch()
    }
  }

  const error = paymentErrorMessage(payState.error ?? quote.error)

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Öz yediyini ödə</DialogTitle>
          <DialogDescription>
            Yalnız sizin sifariş etdiyiniz məhsulları seçin.
          </DialogDescription>
        </DialogHeader>
        {
          <div className="space-y-4">
            <ul className="max-h-64 space-y-2 overflow-y-auto">
              {order.items.map((item) => {
                const isChosen = chosen.includes(item.id)
                return (
                  <li key={item.id}>
                    <button
                      type="button"
                      disabled={item.paid}
                      aria-pressed={isChosen}
                      onClick={() => toggle(item.id)}
                      className={cn(
                        'flex w-full items-center gap-3 rounded-lg border px-3 py-2 text-left text-sm transition-colors',
                        isChosen
                          ? 'border-primary bg-primary/5'
                          : 'border-border hover:bg-muted',
                        item.paid && 'cursor-not-allowed opacity-50',
                      )}
                    >
                      <span
                        className={cn(
                          'flex size-5 shrink-0 items-center justify-center rounded border',
                          isChosen
                            ? 'border-primary bg-primary text-primary-foreground'
                            : 'border-border',
                        )}
                      >
                        {isChosen && <Check className="size-3.5" aria-hidden />}
                      </span>
                      <span className="min-w-0 flex-1 truncate">
                        {item.quantity}&times; {item.name}
                      </span>
                      <span className="shrink-0 tabular-nums">
                        {item.paid
                          ? 'Ödənilib'
                          : formatCurrency(item.total, order.currency)}
                      </span>
                    </button>
                  </li>
                )
              })}
            </ul>

            {needsGuests && (
              <Stepper
                label="Masada neçə nəfərsiniz"
                value={guests}
                min={1}
                max={MAX_GUESTS}
                onChange={setGuests}
              />
            )}

            {chosen.length > 0 ? (
              <AmountDue
                amount={quote.data?.amount}
                currency={order.currency}
                loading={quote.isFetching}
              />
            ) : (
              <p className="text-center text-sm text-muted-foreground">
                Ödəmək üçün ən azı bir məhsul seçin.
              </p>
            )}
            {chosen.length > 0 && quote.data && (
              <TipSection
                amountMinor={quote.data.amount}
                currency={order.currency}
                tipMinor={tipMinor}
                onTipChange={setTipMinor}
              />
            )}
            {error && <ErrorNote>{error}</ErrorNote>}
            <Button
              className="w-full"
              disabled={
                chosen.length === 0 ||
                !quote.data ||
                quote.isFetching ||
                payState.isLoading ||
                (!!quote.data && tipMinor > quote.data.amount)
              }
              onClick={() => void onPay()}
            >
              {payState.isLoading
                ? 'Payriff-ə yönləndirilir...'
                : quote.data && chosen.length > 0
                  ? `${formatCurrency(quote.data.amount + tipMinor, order.currency)} ödə`
                  : 'Ödə'}
            </Button>
          </div>
        }
      </DialogContent>
    </Dialog>
  )
}
