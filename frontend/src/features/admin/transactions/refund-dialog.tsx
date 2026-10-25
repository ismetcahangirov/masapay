import { useState } from 'react'
import { Undo2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { problemReason } from '@/lib/api-errors'
import { formatAmount } from './format'
import {
  type TransactionRow,
  useRefundTransactionMutation,
} from './transactions-api'

// Confirms a full refund of one Payriff payment (#45). The money goes back in
// one call; the list is refetched so the row then shows "Geri qaytarılıb".
/** Why the refund did not go through, from the reason the backend sent. */
function refundErrorMessage(reason: string | null): string {
  switch (reason) {
    case 'REFUND_WINDOW_EXPIRED':
      return 'Ödənişdən 30 gündən çox keçib: Payriff pulu geri qaytarmağa icazə vermir. Ödəniş olduğu kimi qalır.'
    case 'PAYRIFF_REFUSED':
      return 'Payriff geri qaytarmanı rədd etdi. Ödəniş olduğu kimi qalır.'
    case 'PAYRIFF_UNAVAILABLE':
      return 'Payriff hazırda cavab vermir. Bir az sonra yenidən cəhd edin.'
    default:
      return 'Geri qaytarmaq alınmadı. Yenidən cəhd edin.'
  }
}

export function RefundDialog({
  restaurantId,
  transaction,
  onClose,
}: {
  restaurantId: string
  /** The payment to refund; null keeps the dialog closed. */
  transaction: TransactionRow | null
  onClose: () => void
}) {
  const [reason, setReason] = useState('')
  const [refund, refundState] = useRefundTransactionMutation()

  function close() {
    setReason('')
    refundState.reset()
    onClose()
  }

  async function onConfirm() {
    if (!transaction) return
    try {
      await refund({
        restaurantId,
        transactionId: transaction.id,
        reason: reason.trim(),
      }).unwrap()
      close()
    } catch {
      // refundState.isError is shown below.
    }
  }

  return (
    <Dialog
      open={transaction !== null}
      onOpenChange={(open) => !open && close()}
    >
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Ödənişi geri qaytar</DialogTitle>
          <DialogDescription>
            {transaction &&
              `${transaction.tableLabel}: ${formatAmount(transaction.totalAmount, transaction.currency)} müştərinin kartına tam qaytarılacaq. Bu addımı geri almaq olmur.`}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          <Label htmlFor="refund-reason">Səbəb (istəyə bağlı)</Label>
          <Input
            id="refund-reason"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder="Məsələn, səhv məbləğ ödənilib"
            maxLength={200}
          />
        </div>
        {refundState.isError && (
          <p className="text-sm text-destructive">
            {refundErrorMessage(problemReason(refundState.error))}
          </p>
        )}
        <DialogFooter className="gap-2">
          <Button variant="outline" onClick={close}>
            İmtina
          </Button>
          <Button
            variant="destructive"
            onClick={() => void onConfirm()}
            disabled={refundState.isLoading}
          >
            <Undo2 aria-hidden />
            {refundState.isLoading ? 'Qaytarılır...' : 'Geri qaytar'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
