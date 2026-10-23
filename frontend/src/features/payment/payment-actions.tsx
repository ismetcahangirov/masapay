import { useState } from 'react'
import { ListChecks, Users, Wallet } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { orderBalance, type Order } from '@/features/order/types'
import { FullPaymentDialog } from '@/features/payment/full-payment-dialog'
import { EqualSplitDialog } from '@/features/payment/equal-split-dialog'
import { ItemPaymentDialog } from '@/features/payment/item-payment-dialog'
import { PaymentOfferDialog } from '@/features/payment/payment-offer-dialog'
import { paymentOffers } from '@/features/payment/payment-offer'
import {
  type CustomerDish,
  useCustomerMenuQuery,
} from '@/features/ordering/customer-menu-api'

type Mode = 'full' | 'equal' | 'items' | null

// The three ways to pay, shown under the live bill. Each button opens its own
// dialog that first quotes the amount, then pays it. The first time a guest
// goes to pay a bill, the restaurant's extras for it are offered (#183); not
// once an equal split has started, as a new dish would change everyone's share.
export function PaymentActions({
  qrToken,
  order,
}: {
  qrToken: string
  order: Order
}) {
  const [mode, setMode] = useState<Mode>(null)
  const [offering, setOffering] = useState<Mode>(null)
  const [offeredFor, setOfferedFor] = useState<string | null>(null)
  // Kept as offered, so a dish just added stays in the list as "Əlavə olundu".
  const [offerList, setOfferList] = useState<CustomerDish[]>([])
  const menu = useCustomerMenuQuery(qrToken)
  const offers = paymentOffers(
    order,
    (menu.data ?? []).flatMap((category) => category.items),
  )

  function choose(next: Exclude<Mode, null>) {
    if (offers.length > 0 && !order.equalSplit && offeredFor !== order.id) {
      setOfferedFor(order.id)
      setOfferList(offers)
      setOffering(next)
      return
    }
    setMode(next)
  }

  // Nothing to pay on a settled bill, or on one the manager cancelled (#186).
  if (
    order.status === 'paid' ||
    order.status === 'cancelled' ||
    orderBalance(order) === 0
  ) {
    return null
  }

  // Once an equal split has started, paying by items would mix two splits.
  const splitStarted = !!order.equalSplit

  return (
    <div className="w-full max-w-md space-y-3">
      <Button className="h-12 w-full" onClick={() => choose('full')}>
        <Wallet aria-hidden />
        Tam ödə
      </Button>
      <div className="grid grid-cols-2 gap-3">
        <Button className="h-12" onClick={() => choose('equal')}>
          <Users aria-hidden />
          Bərabər böl
        </Button>
        <Button
          className="h-12"
          onClick={() => choose('items')}
          disabled={splitStarted}
          title={
            splitStarted
              ? 'Hesab artıq bərabər bölünür'
              : 'Yalnız öz məhsullarınızı ödəyin'
          }
        >
          <ListChecks aria-hidden />
          Öz yediyim
        </Button>
      </div>
      {splitStarted && (
        <p className="text-center text-xs text-muted-foreground">
          Hesab bərabər bölünür, ona görə "Öz yediyim" bağlıdır. Qalan hissəni
          "Bərabər böl" və ya "Tam ödə" ilə ödəyin.
        </p>
      )}

      <PaymentOfferDialog
        qrToken={qrToken}
        open={offering !== null}
        offers={offerList}
        currency={order.currency}
        onContinue={() => {
          setMode(offering)
          setOffering(null)
        }}
        onClose={() => setOffering(null)}
      />
      <FullPaymentDialog
        qrToken={qrToken}
        currency={order.currency}
        open={mode === 'full'}
        onOpenChange={(open) => setMode(open ? 'full' : null)}
      />
      <EqualSplitDialog
        qrToken={qrToken}
        order={order}
        open={mode === 'equal'}
        onOpenChange={(open) => setMode(open ? 'equal' : null)}
      />
      <ItemPaymentDialog
        qrToken={qrToken}
        order={order}
        open={mode === 'items'}
        onOpenChange={(open) => setMode(open ? 'items' : null)}
      />
    </div>
  )
}
