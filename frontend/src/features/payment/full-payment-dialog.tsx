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

  function close(next: boolean) {
    if (!next) {
      payState.reset()
    }
    onOpenChange(next)
  }

  async function onPay() {
    try {
      goToPayment(await pay(qrToken).unwrap())
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
            {error && <ErrorNote>{error}</ErrorNote>}
            <Button
              className="w-full"
              disabled={!quote.data || quote.isFetching || payState.isLoading}
              onClick={() => void onPay()}
            >
              {payState.isLoading
                ? 'Payriff-ə yönləndirilir...'
                : quote.data
                  ? `${formatCurrency(quote.data.amount, currency)} ödə`
                  : 'Ödə'}
            </Button>
          </div>
        }
      </DialogContent>
    </Dialog>
  )
}
