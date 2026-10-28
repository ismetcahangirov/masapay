import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
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
  useCheckoutEqualMutation,
  useQuoteEqualQuery,
  goToPayment,
} from '@/features/payment/payment-api'
import { AmountDue, ErrorNote, Stepper } from '@/features/payment/payment-parts'
import { paymentErrorMessage } from '@/features/payment/payment-errors'
import { tipAmount } from '@/features/payment/tip-amount'
import { TipSection } from '@/features/payment/tip-section'

const MIN_PARTS = 2
const MAX_PARTS = 50

// "Bərabər böl" (#26): the bill is split into N equal shares and this guest
// pays one or more of them. The first payer fixes N; later payers see it
// locked, so every phone at the table agrees on the share size.
export function EqualSplitDialog({
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
  const { t } = useTranslation('payment')
  const started = order.equalSplit
  const [parts, setParts] = useState(started?.totalParts ?? MIN_PARTS)
  const [partsToPay, setPartsToPay] = useState(1)

  // Another guest may start the split while this dialog is open.
  useEffect(() => {
    if (started) setParts(started.totalParts)
  }, [started])

  const remaining = parts - (started?.paidParts ?? 0)
  const shares = Math.min(partsToPay, Math.max(1, remaining))

  const quote = useQuoteEqualQuery(
    { qrToken, parts, partsToPay: shares },
    { skip: !open },
  )
  const [pay, payState] = useCheckoutEqualMutation()
  const [tipMinor, setTipMinor] = useState(0)

  function close(next: boolean) {
    if (!next) {
      setPartsToPay(1)
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
          parts,
          partsToPay: shares,
          tipAmount: tipAmount(tipMinor),
        }).unwrap(),
      )
    } catch {
      void quote.refetch()
    }
  }

  const error = paymentErrorMessage(payState.error ?? quote.error)

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>{t('equal.title')}</DialogTitle>
          <DialogDescription>
            {started
              ? t('equal.started', {
                  total: started.totalParts,
                  paid: started.paidParts,
                })
              : t('equal.ask')}
          </DialogDescription>
        </DialogHeader>
        {
          <div className="space-y-4">
            <Stepper
              label={t('equal.people')}
              value={parts}
              min={MIN_PARTS}
              max={MAX_PARTS}
              onChange={(value) => {
                setParts(value)
                setPartsToPay(1)
              }}
              disabled={!!started}
            />
            <Stepper
              label={t('equal.partsToPay')}
              value={shares}
              min={1}
              max={Math.max(1, remaining)}
              onChange={setPartsToPay}
            />
            <p className="text-xs text-muted-foreground">
              {started
                ? t('equal.startedHint', { remaining })
                : t('equal.hint', { remaining })}
            </p>
            <AmountDue
              amount={quote.data?.amount}
              currency={order.currency}
              loading={quote.isFetching}
            />
            {quote.data && (
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
                !quote.data ||
                quote.isFetching ||
                payState.isLoading ||
                (!!quote.data && tipMinor > quote.data.amount)
              }
              onClick={() => void onPay()}
            >
              {payState.isLoading
                ? t('pay.redirecting')
                : quote.data
                  ? t('pay.payAmount', {
                      amount: formatCurrency(
                        quote.data.amount + tipMinor,
                        order.currency,
                      ),
                    })
                  : t('pay.pay')}
            </Button>
          </div>
        }
      </DialogContent>
    </Dialog>
  )
}
