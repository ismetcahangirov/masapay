import { useEffect, useState } from 'react'
import {
  CircleCheckBig,
  LoaderCircle,
  Minus,
  Plus,
  ShoppingBasket,
  X,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { formatCurrency } from '@/features/order/format'
import { isRestaurantClosed } from '@/lib/api-errors'
import { ConfirmOrderDialog } from './confirm-order-dialog'
import {
  type CustomerDish,
  useCustomerMenuQuery,
  usePlaceOrderMutation,
} from './customer-menu-api'
import { upsellFor } from './upsell'
import { UpsellDialog } from './upsell-dialog'
import { MAX_QUANTITY, useBasket } from './use-basket'

// Card 1 of the table screen (#140): today's menu and the customer's basket.
// "Əlavə sifariş" is always enabled, so another round can be ordered at any
// time; a confirmed round goes onto the bill and cannot be removed from here.
// Before "Əminsiniz?" the restaurant's suggested extras are offered (#141).
export function MenuOrderCard({
  qrToken,
  currency,
}: {
  qrToken: string
  currency: string
}) {
  const menu = useCustomerMenuQuery(qrToken)
  const basket = useBasket(qrToken)
  const { syncWithMenu } = basket
  const [browsing, setBrowsing] = useState(true)
  const [confirming, setConfirming] = useState(false)
  const [upsell, setUpsell] = useState<CustomerDish[] | null>(null)
  const [addedExtras, setAddedExtras] = useState<Set<string>>(new Set())
  const [justOrdered, setJustOrdered] = useState(false)
  const [placeOrder, placeState] = usePlaceOrderMutation()

  // Whenever today's menu (re)loads, drop basket dishes that left it.
  useEffect(() => {
    if (menu.data) syncWithMenu(menu.data.flatMap((c) => c.items))
  }, [menu.data, syncWithMenu])

  function startNewRound() {
    setJustOrdered(false)
    setBrowsing(true)
  }

  // Extras (#141) are worked out when the customer asks to confirm, and shown
  // first; with nothing to offer, "Əminsiniz?" opens straight away.
  function askToConfirm() {
    const extras = upsellFor(
      basket.entries,
      (menu.data ?? []).flatMap((c) => c.items),
    )
    if (extras.length === 0) {
      setConfirming(true)
      return
    }
    setAddedExtras(new Set())
    setUpsell(extras)
  }

  function addExtra(dish: CustomerDish) {
    basket.change(dish, 1)
    setAddedExtras((current) => new Set(current).add(dish.id))
  }

  function continueToConfirm() {
    setUpsell(null)
    setConfirming(true)
  }

  async function onConfirm() {
    try {
      await placeOrder({
        qrToken,
        items: basket.entries.map(({ dish, quantity }) => ({
          menuItemId: dish.id,
          quantity,
        })),
      }).unwrap()
      basket.clear()
      placeState.reset()
      setConfirming(false)
      setBrowsing(false)
      setJustOrdered(true)
    } catch {
      // placeState.error is shown in the dialog.
    }
  }

  const conflict =
    placeState.error &&
    'status' in placeState.error &&
    placeState.error.status === 409
  const orderError = !placeState.isError
    ? null
    : isRestaurantClosed(placeState.error)
      ? 'Restoran hazırda onlayn sifariş qəbul etmir. Zəhmət olmasa ofisianta müraciət edin.'
      : conflict
        ? 'Bəzi məhsullar artıq mövcud deyil. Menyu yeniləndi, səbəti yoxlayın.'
        : 'Sifarişi göndərmək alınmadı. Yenidən cəhd edin.'

  return (
    <Card className="w-full max-w-md">
      <CardHeader className="flex-row items-start justify-between gap-3 space-y-0">
        <div className="space-y-1">
          <CardTitle className="text-xl">
            <h2>Menyu və sifariş</h2>
          </CardTitle>
          <CardDescription>
            Seçin, yoxlayın və təsdiqləyin. Sifariş masanın hesabına düşür.
          </CardDescription>
          <p className="text-xs text-muted-foreground">
            Təsdiqlənmiş sifarişi ləğv etmək üçün restoran menecerinə müraciət
            edin.
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          className="shrink-0"
          onClick={startNewRound}
        >
          <Plus aria-hidden />
          Əlavə sifariş
        </Button>
      </CardHeader>

      <CardContent className="space-y-4">
        {justOrdered && (
          <div
            role="status"
            className="flex items-center gap-3 rounded-lg bg-success/10 px-4 py-3 text-sm font-medium text-success"
          >
            <CircleCheckBig className="size-5 shrink-0" aria-hidden />
            Sifarişiniz masanın hesabına əlavə olundu.
          </div>
        )}

        {browsing && (
          <>
            {menu.isLoading && (
              <LoaderCircle
                className="mx-auto size-6 animate-spin text-muted-foreground"
                aria-label="Yüklənir"
              />
            )}
            {menu.isError && (
              <p className="text-sm text-destructive">
                Menyunu yükləmək alınmadı.
              </p>
            )}
            {menu.data?.length === 0 && (
              <p className="text-sm text-muted-foreground">
                Bu gün menyuda sifariş ediləcək məhsul yoxdur.
              </p>
            )}
            {menu.data?.map((category) => (
              <section
                key={category.id}
                aria-labelledby={`menu-${category.id}`}
                className="space-y-2"
              >
                <h3
                  id={`menu-${category.id}`}
                  className="text-sm font-semibold uppercase tracking-wide text-muted-foreground"
                >
                  {category.name}
                </h3>
                <ul className="divide-y rounded-lg border">
                  {category.items.map((dish) => (
                    <DishRow
                      key={dish.id}
                      dish={dish}
                      currency={currency}
                      quantity={basket.quantityOf(dish.id)}
                      onChange={(delta) => basket.change(dish, delta)}
                    />
                  ))}
                </ul>
              </section>
            ))}
          </>
        )}
      </CardContent>

      {basket.count > 0 && (
        <CardFooter className="flex-col items-stretch gap-3 border-t pt-6">
          <h3 className="flex items-center gap-2 text-sm font-semibold">
            <ShoppingBasket className="size-4" aria-hidden />
            Səbət
          </h3>
          <ul className="space-y-1 text-sm" aria-label="Səbət">
            {basket.entries.map(({ dish, quantity }) => (
              <li
                key={dish.id}
                className="flex items-center justify-between gap-2"
              >
                <span>
                  {quantity} x {dish.name}
                </span>
                <span className="flex items-center gap-1">
                  <span className="tabular-nums">
                    {formatCurrency(dish.price * quantity, currency)}
                  </span>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="size-8"
                    onClick={() => basket.remove(dish.id)}
                    aria-label={`${dish.name} səbətdən çıxar`}
                  >
                    <X aria-hidden />
                  </Button>
                </span>
              </li>
            ))}
          </ul>
          <Button
            variant="success"
            className="h-12 w-full"
            onClick={askToConfirm}
          >
            Sifarişi təsdiqlə ({formatCurrency(basket.total, currency)})
          </Button>
        </CardFooter>
      )}

      <UpsellDialog
        open={upsell !== null}
        extras={upsell ?? []}
        added={addedExtras}
        currency={currency}
        onAdd={addExtra}
        onContinue={continueToConfirm}
        onClose={() => setUpsell(null)}
      />

      <ConfirmOrderDialog
        open={confirming}
        entries={basket.entries}
        total={basket.total}
        currency={currency}
        busy={placeState.isLoading}
        error={orderError}
        onConfirm={() => void onConfirm()}
        onClose={() => {
          placeState.reset()
          setConfirming(false)
        }}
      />
    </Card>
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
    <li className="flex items-start justify-between gap-3 p-3">
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
            className="size-8"
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
            className="size-8"
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
