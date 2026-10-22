import { Check, Plus } from 'lucide-react'
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
import type { CustomerDish } from './customer-menu-api'

// Shown right before "Əminsiniz?" (#141): extras the restaurant suggests for
// what is in the basket. One click adds an extra; "Xeyr, təşəkkürlər" or
// "Davam et" goes on to the confirm step either way.
export function UpsellDialog({
  open,
  extras,
  added,
  currency,
  onAdd,
  onContinue,
  onClose,
}: {
  open: boolean
  extras: CustomerDish[]
  /** Extras added from this dialog, shown as done. */
  added: Set<string>
  currency: string
  onAdd: (dish: CustomerDish) => void
  onContinue: () => void
  onClose: () => void
}) {
  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Bunu da istərdiniz?</DialogTitle>
          <DialogDescription>
            Seçdiyiniz məhsullarla yaxşı gedir.
          </DialogDescription>
        </DialogHeader>
        <ul className="divide-y rounded-md border">
          {extras.map((dish) => {
            const done = added.has(dish.id)
            return (
              <li
                key={dish.id}
                className="flex items-center justify-between gap-3 px-3 py-2"
              >
                <span className="min-w-0">
                  <span className="block text-sm font-medium">{dish.name}</span>
                  <span className="text-xs tabular-nums text-muted-foreground">
                    {formatCurrency(dish.price, currency)}
                  </span>
                </span>
                <Button
                  variant={done ? 'secondary' : 'outline'}
                  size="sm"
                  disabled={done}
                  onClick={() => onAdd(dish)}
                  aria-label={
                    done ? `${dish.name} əlavə olundu` : `${dish.name} əlavə et`
                  }
                >
                  {done ? <Check aria-hidden /> : <Plus aria-hidden />}
                  {done ? 'Əlavə olundu' : 'Əlavə et'}
                </Button>
              </li>
            )
          })}
        </ul>
        <DialogFooter>
          <Button variant="success" className="w-full" onClick={onContinue}>
            {added.size > 0 ? 'Davam et' : 'Xeyr, təşəkkürlər'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
