import { useState } from 'react'
import { ListChecks, Users, Wallet } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { orderBalance, type Order } from '@/features/order/types'
import { FullPaymentDialog } from '@/features/payment/full-payment-dialog'
import { EqualSplitDialog } from '@/features/payment/equal-split-dialog'
import { ItemPaymentDialog } from '@/features/payment/item-payment-dialog'

type Mode = 'full' | 'equal' | 'items' | null

// The three ways to pay, shown under the live bill. Each button opens its own
// dialog that first quotes the amount, then pays it.
export function PaymentActions({
  qrToken,
  order,
}: {
  qrToken: string
  order: Order
}) {
  const [mode, setMode] = useState<Mode>(null)

  if (order.status === 'paid' || orderBalance(order) === 0) {
    return null
  }

  // Once an equal split has started, paying by items would mix two splits.
  const splitStarted = !!order.equalSplit

  return (
    <div className="w-full max-w-md space-y-3">
      <Button className="h-12 w-full" onClick={() => setMode('full')}>
        <Wallet aria-hidden />
        Tam ödə
      </Button>
      <div className="grid grid-cols-2 gap-3">
        <Button className="h-12" onClick={() => setMode('equal')}>
          <Users aria-hidden />
          Bərabər böl
        </Button>
        <Button
          className="h-12"
          onClick={() => setMode('items')}
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
