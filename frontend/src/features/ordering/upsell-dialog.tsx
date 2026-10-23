import { Check, Minus, Plus } from 'lucide-react'
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

/** "Ketçup və ya Su istəyirsiniz?" from the names of the extras asked about. */
function askQuestion(asked: CustomerDish[]): string {
  const names = asked.map((dish) => dish.name)
  if (names.length <= 1) return `${names[0] ?? 'Əlavə'} istəyirsiniz?`
  return `${names.slice(0, -1).join(', ')} və ya ${names.at(-1)} istəyirsiniz?`
}

// Shown right before "Əminsiniz?". Two kinds of extras in one dialog:
// - suggestions for what is in the basket (#141): one click adds one;
// - extras the restaurant asks every customer about, KFC style, such as
//   ketchup or water (#185): a quantity for each.
// "Xeyr, təşəkkürlər" (nothing taken) or "Davam et" goes on to the confirm
// step either way.
export function UpsellDialog({
  open,
  extras,
  asked = [],
  quantityOf = () => 0,
  onChangeAsked = () => {},
  added,
  currency,
  onAdd,
  onContinue,
  onClose,
}: {
  open: boolean
  /** Suggestions for the basket (#141). */
  extras: CustomerDish[]
  /** Extras asked about before every order (#185). */
  asked?: CustomerDish[]
  /** How many of a dish are in the basket now. */
  quantityOf?: (dishId: string) => number
  onChangeAsked?: (dish: CustomerDish, delta: number) => void
  /** Suggestions added from this dialog, shown as done. */
  added: Set<string>
  currency: string
  onAdd: (dish: CustomerDish) => void
  onContinue: () => void
  onClose: () => void
}) {
  const tookSomething =
    added.size > 0 || asked.some((dish) => quantityOf(dish.id) > 0)
  const onlyAsked = extras.length === 0 && asked.length > 0

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>
            {onlyAsked ? askQuestion(asked) : 'Bunu da istərdiniz?'}
          </DialogTitle>
          <DialogDescription>
            {onlyAsked
              ? 'İstəsəniz sayını seçin, sifarişə əlavə olunacaq.'
              : 'Seçdiyiniz məhsullarla yaxşı gedir.'}
          </DialogDescription>
        </DialogHeader>
        {extras.length > 0 && (
          <ul className="divide-y rounded-md border">
            {extras.map((dish) => {
              const done = added.has(dish.id)
              return (
                <li
                  key={dish.id}
                  className="flex items-center justify-between gap-3 px-3 py-2"
                >
                  <span className="min-w-0">
                    <span className="block text-sm font-medium">
                      {dish.name}
                    </span>
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
                      done
                        ? `${dish.name} əlavə olundu`
                        : `${dish.name} əlavə et`
                    }
                  >
                    {done ? <Check aria-hidden /> : <Plus aria-hidden />}
                    {done ? 'Əlavə olundu' : 'Əlavə et'}
                  </Button>
                </li>
              )
            })}
          </ul>
        )}
        {asked.length > 0 && (
          <section aria-label={askQuestion(asked)} className="space-y-2">
            {!onlyAsked && (
              <p className="text-sm font-medium">{askQuestion(asked)}</p>
            )}
            <ul className="divide-y rounded-md border">
              {asked.map((dish) => {
                const quantity = quantityOf(dish.id)
                return (
                  <li
                    key={dish.id}
                    className="flex items-center justify-between gap-3 px-3 py-2"
                  >
                    <span className="min-w-0">
                      <span className="block text-sm font-medium">
                        {dish.name}
                      </span>
                      <span className="text-xs tabular-nums text-muted-foreground">
                        {dish.price === 0
                          ? 'Pulsuz'
                          : formatCurrency(dish.price, currency)}
                      </span>
                    </span>
                    <span className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="icon"
                        className="size-10"
                        disabled={quantity === 0}
                        onClick={() => onChangeAsked(dish, -1)}
                        aria-label={`${dish.name} azalt`}
                      >
                        <Minus aria-hidden />
                      </Button>
                      <span
                        className="w-5 text-center tabular-nums"
                        aria-label={`${dish.name} sayı`}
                      >
                        {quantity}
                      </span>
                      <Button
                        variant="outline"
                        size="icon"
                        className="size-10"
                        onClick={() => onChangeAsked(dish, 1)}
                        aria-label={`${dish.name} artır`}
                      >
                        <Plus aria-hidden />
                      </Button>
                    </span>
                  </li>
                )
              })}
            </ul>
          </section>
        )}
        <DialogFooter>
          <Button variant="success" className="w-full" onClick={onContinue}>
            {tookSomething ? 'Davam et' : 'Xeyr, təşəkkürlər'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
