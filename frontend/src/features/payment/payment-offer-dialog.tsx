import { useState } from 'react'
import { Check, Plus, Wallet } from 'lucide-react'
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
import {
  type CustomerDish,
  usePlaceOrderMutation,
} from '@/features/ordering/customer-menu-api'

// Shown when the guest goes to pay, before the payment itself (#183): "Bu
// məhsulları da istəyirsiniz?" with the extras the restaurant suggests. "Əlavə
// et" puts one on the bill at once, so the payment that follows includes it;
// "Xeyr, ödə" goes on to paying unchanged.
export function PaymentOfferDialog({
  qrToken,
  open,
  offers,
  currency,
  onContinue,
  onClose,
}: {
  qrToken: string
  open: boolean
  offers: CustomerDish[]
  currency: string
  onContinue: () => void
  onClose: () => void
}) {
  const [added, setAdded] = useState<Set<string>>(new Set())
  const [placeOrder, placeState] = usePlaceOrderMutation()

  async function add(dish: CustomerDish) {
    try {
      await placeOrder({
        qrToken,
        items: [{ menuItemId: dish.id, quantity: 1 }],
      }).unwrap()
      setAdded((current) => new Set(current).add(dish.id))
    } catch {
      // placeState.isError is shown below.
    }
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Bu məhsulları da istəyirsiniz?</DialogTitle>
          <DialogDescription>
            Ödəməzdən əvvəl: əlavə etsəniz hesaba düşür və ödənişə daxil olur.
          </DialogDescription>
        </DialogHeader>
        <ul className="divide-y rounded-md border">
          {offers.map((dish) => {
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
                  disabled={done || placeState.isLoading}
                  onClick={() => void add(dish)}
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
        {placeState.isError && (
          <p className="text-sm text-destructive">
            Əlavə etmək alınmadı. Məhsul artıq mövcud olmaya bilər.
          </p>
        )}
        <DialogFooter>
          <Button
            variant="success"
            className="w-full"
            disabled={placeState.isLoading}
            onClick={onContinue}
          >
            <Wallet aria-hidden />
            {added.size > 0 ? 'Ödənişə keç' : 'Xeyr, ödə'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
