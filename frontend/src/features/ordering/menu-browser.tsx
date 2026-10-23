import { useState } from 'react'
import { LoaderCircle, Minus, Plus, ShoppingBasket, X } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { formatCurrency } from '@/features/order/format'
import { cn } from '@/lib/utils'
import type { CustomerCategory, CustomerDish } from './customer-menu-api'
import { MAX_QUANTITY, type BasketEntry } from './use-basket'

// The customer's menu (#180), opened from the "Menyu" button: categories on
// the left, the chosen category's dishes in the middle, and what was picked on
// the right with its subtotal. On a phone the categories become a row of tabs
// at the top and the picked dishes a panel at the bottom. Confirming closes
// the menu and goes on with the usual steps (extras, then "Əminsiniz?").
export function MenuBrowser({
  open,
  categories,
  loading,
  failed,
  currency,
  entries,
  total,
  quantityOf,
  onChange,
  onRemove,
  onConfirm,
  onClose,
}: {
  open: boolean
  categories: CustomerCategory[]
  loading: boolean
  failed: boolean
  currency: string
  entries: BasketEntry[]
  /** Basket total in minor units. */
  total: number
  quantityOf: (dishId: string) => number
  onChange: (dish: CustomerDish, delta: number) => void
  onRemove: (dishId: string) => void
  onConfirm: () => void
  onClose: () => void
}) {
  const [chosenId, setChosenId] = useState<string | null>(null)
  const active =
    categories.find((category) => category.id === chosenId) ?? categories[0]
  const count = entries.reduce((sum, entry) => sum + entry.quantity, 0)

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="flex h-[92svh] max-w-6xl flex-col gap-0 overflow-hidden p-0">
        <DialogHeader className="border-b px-4 py-3 text-left sm:px-6">
          <DialogTitle>Menyu</DialogTitle>
          <DialogDescription>
            Kateqoriya seçin, yeməkləri əlavə edin, sonra sifarişi təsdiqləyin.
          </DialogDescription>
        </DialogHeader>

        {loading && (
          <LoaderCircle
            className="mx-auto my-10 size-6 animate-spin text-muted-foreground"
            aria-label="Yüklənir"
          />
        )}
        {failed && (
          <p className="p-6 text-sm text-destructive">
            Menyunu yükləmək alınmadı.
          </p>
        )}
        {!loading && !failed && categories.length === 0 && (
          <p className="p-6 text-sm text-muted-foreground">
            Bu gün menyuda sifariş ediləcək məhsul yoxdur.
          </p>
        )}

        {active && (
          <div className="grid min-h-0 flex-1 grid-rows-[auto_1fr_auto] md:grid-cols-[13rem_1fr_19rem] md:grid-rows-1">
            <nav
              aria-label="Kateqoriyalar"
              className="flex gap-2 overflow-x-auto border-b p-3 md:flex-col md:overflow-y-auto md:overflow-x-visible md:border-b-0 md:border-r"
            >
              {categories.map((category) => {
                const selected = category.id === active.id
                return (
                  <Button
                    key={category.id}
                    variant={selected ? 'default' : 'ghost'}
                    aria-pressed={selected}
                    className="shrink-0 justify-start md:w-full"
                    onClick={() => setChosenId(category.id)}
                  >
                    {category.name}
                  </Button>
                )
              })}
            </nav>

            <section
              aria-label={active.name}
              className="min-h-0 overflow-y-auto p-3 sm:p-4"
            >
              <h3 className="mb-2 hidden text-sm font-semibold uppercase tracking-wide text-muted-foreground md:block">
                {active.name}
              </h3>
              <ul className="divide-y rounded-lg border">
                {active.items.map((dish) => (
                  <DishRow
                    key={dish.id}
                    dish={dish}
                    currency={currency}
                    quantity={quantityOf(dish.id)}
                    onChange={(delta) => onChange(dish, delta)}
                  />
                ))}
              </ul>
            </section>

            <aside
              aria-label="Seçilənlər"
              className="flex max-h-[38svh] flex-col gap-3 border-t bg-background p-3 sm:p-4 md:max-h-none md:border-l md:border-t-0"
            >
              <h3 className="flex items-center gap-2 text-sm font-semibold">
                <ShoppingBasket className="size-4" aria-hidden />
                Seçilənlər
                {count > 0 && <Badge variant="secondary">{count}</Badge>}
              </h3>
              {entries.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  Hələ heç nə seçilməyib.
                </p>
              ) : (
                <ul className="min-h-0 flex-1 space-y-2 overflow-y-auto text-sm">
                  {entries.map(({ dish, quantity }) => (
                    <li
                      key={dish.id}
                      className="flex items-center justify-between gap-2"
                    >
                      <span className="min-w-0">
                        <span className="block truncate font-medium">
                          {dish.name}
                        </span>
                        <span className="tabular-nums text-muted-foreground">
                          {quantity} x {formatCurrency(dish.price, currency)}
                        </span>
                      </span>
                      <span className="flex shrink-0 items-center gap-1">
                        <Button
                          variant="outline"
                          size="icon"
                          className="size-10"
                          onClick={() => onChange(dish, -1)}
                          aria-label={`Seçilən ${dish.name} azalt`}
                        >
                          <Minus aria-hidden />
                        </Button>
                        <Button
                          variant="outline"
                          size="icon"
                          className="size-10"
                          disabled={quantity >= MAX_QUANTITY}
                          onClick={() => onChange(dish, 1)}
                          aria-label={`Seçilən ${dish.name} artır`}
                        >
                          <Plus aria-hidden />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="size-10"
                          onClick={() => onRemove(dish.id)}
                          aria-label={`${dish.name} səbətdən çıxar`}
                        >
                          <X aria-hidden />
                        </Button>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
              <div className="flex items-center justify-between border-t pt-3 text-sm font-medium">
                <span>Ara cəm</span>
                <span className="tabular-nums">
                  {formatCurrency(total, currency)}
                </span>
              </div>
              <Button
                variant="success"
                className="h-12 w-full"
                disabled={entries.length === 0}
                onClick={onConfirm}
              >
                Sifarişi təsdiqlə ({formatCurrency(total, currency)})
              </Button>
            </aside>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}

function DishRow({
  dish,
  currency,
  quantity,
  onChange,
}: {
  dish: CustomerDish
  currency: string
  quantity: number
  onChange: (delta: number) => void
}) {
  return (
    <li
      className={cn(
        'flex items-start justify-between gap-3 p-3',
        quantity > 0 && 'bg-success/5',
      )}
    >
      <div className="min-w-0 space-y-0.5">
        <p className="flex flex-wrap items-center gap-2 font-medium">
          {dish.name}
          {dish.components.length > 0 && (
            <Badge variant="secondary">Kombo</Badge>
          )}
        </p>
        {dish.components.length > 0 && (
          <p className="text-xs">
            {dish.components.map((c) => `${c.quantity} x ${c.name}`).join(', ')}
          </p>
        )}
        {dish.description && (
          <p className="text-xs text-muted-foreground">{dish.description}</p>
        )}
        <p className="text-sm tabular-nums">
          {formatCurrency(dish.price, currency)}
        </p>
      </div>
      {quantity === 0 ? (
        <Button
          variant="outline"
          size="sm"
          className="shrink-0"
          onClick={() => onChange(1)}
          aria-label={`${dish.name} əlavə et`}
        >
          <Plus aria-hidden />
          Əlavə et
        </Button>
      ) : (
        <div className="flex shrink-0 items-center gap-1">
          <Button
            variant="outline"
            size="icon"
            className="size-10"
            onClick={() => onChange(-1)}
            aria-label={`${dish.name} azalt`}
          >
            <Minus aria-hidden />
          </Button>
          <span
            className="w-6 text-center text-sm tabular-nums"
            aria-label={`${dish.name} sayı`}
          >
            {quantity}
          </span>
          <Button
            variant="outline"
            size="icon"
            className="size-10"
            onClick={() => onChange(1)}
            disabled={quantity >= MAX_QUANTITY}
            aria-label={`${dish.name} artır`}
          >
            <Plus aria-hidden />
          </Button>
        </div>
      )}
    </li>
  )
}
