import { Minus, Plus, UtensilsCrossed } from 'lucide-react'
import { useTranslation } from 'react-i18next'
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
import type { CustomerDish } from './customer-menu-api'
import { MAX_QUANTITY } from './use-basket'

// One dish on the customer's menu: a row with its photo and main ingredients,
// and a larger card opened by tapping the row, so a guest who does not know
// the dish can see what it is before adding it.

/** "toyuq, düyü, zəfəran" as separate items. */
function ingredientList(ingredients: string | null): string[] {
  return (ingredients ?? '')
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item !== '')
}

function DishPicture({
  dish,
  className,
}: {
  dish: CustomerDish
  className: string
}) {
  if (dish.imageUrl) {
    return (
      <img
        src={dish.imageUrl}
        alt=""
        loading="lazy"
        className={cn('shrink-0 bg-muted object-cover', className)}
      />
    )
  }
  return (
    <div
      className={cn(
        'flex shrink-0 items-center justify-center bg-muted text-muted-foreground',
        className,
      )}
      aria-hidden
    >
      <UtensilsCrossed className="size-1/3" />
    </div>
  )
}

function QuantityControls({
  dish,
  quantity,
  onChange,
  size = 'sm',
}: {
  dish: CustomerDish
  quantity: number
  onChange: (delta: number) => void
  size?: 'sm' | 'lg'
}) {
  const { t } = useTranslation('customer')
  if (quantity === 0) {
    return (
      <Button
        variant={size === 'lg' ? 'success' : 'outline'}
        size={size === 'lg' ? 'default' : 'sm'}
        className={cn('shrink-0', size === 'lg' && 'h-12 flex-1')}
        onClick={() => onChange(1)}
        aria-label={t('dish.addAria', { name: dish.name })}
      >
        <Plus aria-hidden />
        {t('dish.add')}
      </Button>
    )
  }
  return (
    <div className="flex shrink-0 items-center gap-1">
      <Button
        variant="outline"
        size="icon"
        className="size-10"
        onClick={() => onChange(-1)}
        aria-label={t('dish.decreaseAria', { name: dish.name })}
      >
        <Minus aria-hidden />
      </Button>
      <span
        className="w-6 text-center text-sm tabular-nums"
        aria-label={t('dish.quantityAria', { name: dish.name })}
      >
        {quantity}
      </span>
      <Button
        variant="outline"
        size="icon"
        className="size-10"
        onClick={() => onChange(1)}
        disabled={quantity >= MAX_QUANTITY}
        aria-label={t('dish.increaseAria', { name: dish.name })}
      >
        <Plus aria-hidden />
      </Button>
    </div>
  )
}

function ComboLine({ dish }: { dish: CustomerDish }) {
  if (dish.components.length === 0) return null
  return (
    <p className="text-xs">
      {dish.components.map((c) => `${c.quantity} x ${c.name}`).join(', ')}
    </p>
  )
}

export function DishRow({
  dish,
  currency,
  quantity,
  onChange,
  onOpen,
}: {
  dish: CustomerDish
  currency: string
  quantity: number
  onChange: (delta: number) => void
  onOpen: () => void
}) {
  const { t } = useTranslation('customer')
  return (
    <li
      className={cn(
        'flex items-start justify-between gap-3 p-3',
        quantity > 0 && 'bg-success/5',
      )}
    >
      <button
        type="button"
        onClick={onOpen}
        aria-label={t('dish.aboutAria', { name: dish.name })}
        className="flex min-w-0 flex-1 items-start gap-3 rounded-md text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        <DishPicture dish={dish} className="size-20 rounded-md sm:size-24" />
        <span className="min-w-0 flex-1 space-y-0.5">
          <span className="flex flex-wrap items-center gap-2 font-medium">
            {dish.name}
            {dish.components.length > 0 && (
              <Badge variant="secondary">{t('dish.combo')}</Badge>
            )}
          </span>
          <ComboLine dish={dish} />
          {dish.ingredients && (
            <span className="line-clamp-2 block text-xs">
              {dish.ingredients}
            </span>
          )}
          {dish.description && (
            <span className="line-clamp-1 block text-xs text-muted-foreground">
              {dish.description}
            </span>
          )}
          <span className="block text-sm tabular-nums">
            {formatCurrency(dish.price, currency)}
          </span>
        </span>
      </button>
      <QuantityControls dish={dish} quantity={quantity} onChange={onChange} />
    </li>
  )
}

export function DishDetail({
  dish,
  currency,
  quantity,
  onChange,
  onClose,
}: {
  dish: CustomerDish | null
  currency: string
  quantity: number
  onChange: (delta: number) => void
  onClose: () => void
}) {
  const { t } = useTranslation('customer')
  const ingredients = dish ? ingredientList(dish.ingredients) : []
  return (
    <Dialog open={dish !== null} onOpenChange={(next) => !next && onClose()}>
      {/* The photo goes edge to edge below a top strip of its own for the close button, so the X never sits on the picture. */}
      <DialogContent className="max-h-[92svh] max-w-md gap-0 overflow-y-auto p-0 pt-12 sm:p-0 sm:pt-12">
        {dish && (
          <>
            <DishPicture dish={dish} className="aspect-[4/3] w-full" />
            <div className="space-y-4 p-4 sm:p-6">
              <DialogHeader className="space-y-1 text-left">
                <DialogTitle className="flex flex-wrap items-center gap-2 pr-6">
                  {dish.name}
                  {dish.components.length > 0 && (
                    <Badge variant="secondary">{t('dish.combo')}</Badge>
                  )}
                </DialogTitle>
                <DialogDescription className="text-base font-medium tabular-nums text-foreground">
                  {formatCurrency(dish.price, currency)}
                </DialogDescription>
              </DialogHeader>
              <ComboLine dish={dish} />
              {ingredients.length > 0 && (
                <section className="space-y-2">
                  <h4 className="text-sm font-semibold">
                    {t('dish.ingredients')}
                  </h4>
                  <ul className="flex flex-wrap gap-1.5">
                    {ingredients.map((item, index) => (
                      <li key={`${index}-${item}`}>
                        <Badge variant="outline" className="font-normal">
                          {item}
                        </Badge>
                      </li>
                    ))}
                  </ul>
                </section>
              )}
              {dish.description && (
                <p className="text-sm text-muted-foreground">
                  {dish.description}
                </p>
              )}
              <div className="flex items-center justify-end gap-2">
                <QuantityControls
                  dish={dish}
                  quantity={quantity}
                  onChange={onChange}
                  size="lg"
                />
              </div>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}
