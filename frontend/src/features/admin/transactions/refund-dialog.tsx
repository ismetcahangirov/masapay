import { useState } from 'react'
import { useTranslation } from 'react-i18next'
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
import i18n from '@/i18n'
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
      return i18n.t('transactions:refund.errors.windowExpired')
    case 'PAYRIFF_REFUSED':
      return i18n.t('transactions:refund.errors.refused')
    case 'PAYRIFF_UNAVAILABLE':
      return i18n.t('transactions:refund.errors.unavailable')
    default:
      return i18n.t('transactions:refund.errors.generic')
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
  const { t } = useTranslation('transactions')
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
          <DialogTitle>{t('refund.title')}</DialogTitle>
          <DialogDescription>
            {transaction &&
              t('refund.description', {
                table: transaction.tableLabel,
                amount: formatAmount(
                  transaction.totalAmount,
                  transaction.currency,
                ),
              })}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          <Label htmlFor="refund-reason">{t('refund.reason')}</Label>
          <Input
            id="refund-reason"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder={t('refund.reasonPlaceholder')}
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
            {t('refund.cancel')}
          </Button>
          <Button
            variant="destructive"
            onClick={() => void onConfirm()}
            disabled={refundState.isLoading}
          >
            <Undo2 aria-hidden />
            {refundState.isLoading ? t('refund.refunding') : t('refund.refund')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
