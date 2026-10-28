import { TriangleAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { formatCurrency } from '@/features/order/format'
import type { BasketEntry } from './use-basket'

// "Are you sure?" before the basket goes onto the table's bill (#140). After
// this step the customer can no longer remove what they ordered.
export function ConfirmOrderDialog({
  open,
  entries,
  total,
  currency,
  busy,
  error,
  onConfirm,
  onClose,
}: {
  open: boolean
  entries: BasketEntry[]
  /** Minor units. */
  total: number
  currency: string
  busy: boolean
  error: string | null
  onConfirm: () => void
  onClose: () => void
}) {
  const { t } = useTranslation('customer')
  return (
    <Dialog open={open} onOpenChange={(next) => !next && !busy && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{t('confirmOrder.title')}</DialogTitle>
          <DialogDescription>{t('confirmOrder.description')}</DialogDescription>
        </DialogHeader>
        <ul className="divide-y rounded-md border text-sm">
          {entries.map(({ dish, quantity }) => (
            <li key={dish.id} className="flex justify-between gap-3 px-3 py-2">
              <span>
                {quantity} x {dish.name}
              </span>
              <span className="tabular-nums">
                {formatCurrency(dish.price * quantity, currency)}
              </span>
            </li>
          ))}
          <li className="flex justify-between gap-3 px-3 py-2 font-semibold">
            <span>{t('confirmOrder.total')}</span>
            <span className="tabular-nums">
              {formatCurrency(total, currency)}
            </span>
          </li>
        </ul>
        <p className="flex items-start gap-2 text-sm text-muted-foreground">
          <TriangleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
          {t('confirmOrder.warning')}
        </p>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <DialogFooter className="gap-2">
          <Button variant="outline" onClick={onClose} disabled={busy}>
            {t('confirmOrder.back')}
          </Button>
          <Button variant="success" onClick={onConfirm} disabled={busy}>
            {busy ? t('confirmOrder.sending') : t('confirmOrder.confirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
