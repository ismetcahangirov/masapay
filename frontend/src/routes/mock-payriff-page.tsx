import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { CreditCard, FlaskConical } from 'lucide-react'
import { formatCurrency } from '@/features/order/format'
import { toMinor } from '@/features/order/order-api'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

// LOCAL DEVELOPMENT ONLY (#37): stands in for Payriff's payment page when the
// backend runs with MASAPAY_PAYRIFF_MOCK=true. "Ödə" or "İmtina et" posts the
// same signed callback Payriff would send to our webhook, then continues to the
// normal result page. Rendered only in `npm run dev`.
export function MockPayriffPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [sending, setSending] = useState(false)
  const [failed, setFailed] = useState(false)

  const tx = params.get('tx') ?? ''
  const sig = params.get('sig') ?? ''
  const order = params.get('order') ?? ''
  const amount = Number(params.get('amount') ?? '0')
  const currency = params.get('currency') ?? 'AZN'

  async function answer(paymentStatus: 'APPROVED' | 'DECLINED') {
    setSending(true)
    setFailed(false)
    try {
      const query = new URLSearchParams({ tx, sig })
      const response = await fetch(`/api/public/payriff/webhook?${query}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ payload: { orderId: order, paymentStatus } }),
      })
      if (!response.ok) throw new Error(String(response.status))
      navigate(`/payment/result?tx=${encodeURIComponent(tx)}`, {
        replace: true,
      })
    } catch {
      setFailed(true)
      setSending(false)
    }
  }

  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/40 p-6">
      <Card className="w-full max-w-sm">
        <CardHeader className="text-center">
          <p className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
            <FlaskConical className="size-3.5" aria-hidden />
            Test ödəniş səhifəsi (Payriff əvəzi)
          </p>
          <CardTitle className="flex items-center justify-center gap-2">
            <CreditCard className="size-5" aria-hidden />
            {formatCurrency(toMinor(amount), currency)}
          </CardTitle>
          <CardDescription>
            Real kart tələb olunmur. Nəticəni seçin.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-2">
          <Button
            className="w-full"
            disabled={sending}
            onClick={() => void answer('APPROVED')}
          >
            Ödə (uğurlu)
          </Button>
          <Button
            variant="outline"
            className="w-full"
            disabled={sending}
            onClick={() => void answer('DECLINED')}
          >
            İmtina et (uğursuz)
          </Button>
          {failed && (
            <p className="text-center text-xs text-destructive">
              Cavab göndərilmədi. Backend MASAPAY_PAYRIFF_MOCK=true ilə işə
              salınmalıdır.
            </p>
          )}
        </CardContent>
      </Card>
    </main>
  )
}
