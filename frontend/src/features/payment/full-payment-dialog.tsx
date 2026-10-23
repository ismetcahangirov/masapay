import { useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { formatCurrency } from '@/features/order/format'
import {
  useCheckoutFullMutation,
  useQuoteFullQuery,
  goToPayment,
} from '@/features/payment/payment-api'
import { AmountDue, ErrorNote } from '@/features/payment/payment-parts'
import { paymentErrorMessage } from '@/features/payment/payment-errors'
import { tipAmount } from '@/features/payment/tip-amount'
import { TipSection } from '@/features/payment/tip-section'

// "Tam ödə" (#24): one guest pays everything that is still outstanding.
export function FullPaymentDialog({
  qrToken,
  currency,
  open,
  onOpenChange,
}: {
  qrToken: string
  currency: string
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const quote = useQuoteFullQuery(qrToken, { skip: !open })
  const [pay, payState] = useCheckoutFullMutation()
  const [tipMinor, setTipMinor] = useState(0)

  function close(next: boolean) {
    if (!next) {
      payState.reset()
      setTipMinor(0)
    }
    onOpenChange(next)
  }

  async function onPay() {
    try {
      goToPayment(
        await pay({ qrToken, tipAmount: tipAmount(tipMinor) }).unwrap(),
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
          <DialogTitle>Tam ödə</DialogTitle>
          <DialogDescription>
            Hesabın qalan hissəsinin hamısını siz ödəyirsiniz.
          </DialogDescription>
        </DialogHeader>
        {
          <div className="space-y-4">
            <AmountDue
              amount={quote.data?.amount}
              currency={currency}
              loading={quote.isFetching}
            />
            {quote.data && (
              <TipSection
                amountMinor={quote.data.amount}
                currency={currency}
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
                ? 'Payriff-ə yönləndirilir...'
                : quote.data
                  ? `${formatCurrency(quote.data.amount + tipMinor, currency)} ödə`
                  : 'Ödə'}
            </Button>
          </div>
        }
      </DialogContent>
    </Dialog>
  )
}
