import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Ban } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { problemReason } from '@/lib/api-errors'
import { useCancelOrderMutation } from './orders-api'

// Cancelling a table's confirmed order (#186), for managers only. It asks for
// a reason and a second click; a bill with a payment on it is refused by the
// backend, which needs a refund (by the super admin) first.
export function CancelOrderSection({
  restaurantId,
  tableId,
  onCancelled,
}: {
  restaurantId: string
  tableId: string
  onCancelled: () => void
}) {
  const { t } = useTranslation('orders')
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [cancel, cancelState] = useCancelOrderMutation()

  async function onConfirm() {
    if (!reason.trim()) return
    try {
      await cancel({ restaurantId, tableId, reason: reason.trim() }).unwrap()
      setOpen(false)
      setReason('')
      onCancelled()
    } catch {
      // cancelState.error is shown below.
    }
  }

  const error = !cancelState.isError
    ? null
    : problemReason(cancelState.error) === 'ORDER_HAS_PAYMENTS'
      ? t('cancel.hasPayments')
      : t('cancel.error')

  if (!open) {
    return (
      <section className="border-t pt-4">
        <Button
          type="button"
          variant="outline"
          className="w-full text-destructive"
          onClick={() => setOpen(true)}
        >
          <Ban aria-hidden />
          {t('cancel.open')}
        </Button>
      </section>
    )
  }

  return (
    <section
      aria-label={t('cancel.sectionLabel')}
      className="space-y-3 rounded-lg border border-destructive p-4"
    >
      <p className="text-sm font-medium">{t('cancel.warning')}</p>
      <div className="space-y-1">
        <Label htmlFor="cancel-reason">{t('cancel.reason')}</Label>
        <Textarea
          id="cancel-reason"
          value={reason}
          maxLength={500}
          onChange={(event) => setReason(event.target.value)}
          placeholder={t('cancel.reasonPlaceholder')}
        />
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
      <div className="flex justify-end gap-2">
        <Button
          type="button"
          variant="outline"
          onClick={() => {
            setOpen(false)
            cancelState.reset()
          }}
        >
          {t('cancel.back')}
        </Button>
        <Button
          type="button"
          variant="destructive"
          disabled={!reason.trim() || cancelState.isLoading}
          onClick={() => void onConfirm()}
        >
          {cancelState.isLoading ? t('cancel.cancelling') : t('cancel.confirm')}
        </Button>
      </div>
    </section>
  )
}
