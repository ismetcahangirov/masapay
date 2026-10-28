import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { LucideIcon } from 'lucide-react'
import {
  CircleAlert,
  CircleCheckBig,
  CircleX,
  Clock,
  LoaderCircle,
  RotateCw,
  WifiOff,
} from 'lucide-react'
import { usePaymentStatusQuery } from '@/features/payment/payment-api'
import { formatCurrency } from '@/features/order/format'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { cn } from '@/lib/utils'

// Where Payriff sends the guest back (#37): /payment/result?tx=<transactionId>.
// The page asks the backend for the payment's status every 2 seconds until
// Payriff's confirmation (the webhook) has arrived, then shows success or
// failure. Each state has its own icon and text; an offline phone gets a clear
// "no connection" screen with a retry button instead of a spinner forever.

const POLL_MS = 2000
const GIVE_UP_MS = 90_000

type Screen = 'checking' | 'success' | 'failed' | 'slow' | 'offline' | 'unknown'

const SCREENS: Record<Screen, { icon: LucideIcon; tone: string }> = {
  checking: {
    icon: Clock,
    tone: 'bg-muted text-muted-foreground',
  },
  success: {
    icon: CircleCheckBig,
    tone: 'bg-success/15 text-success',
  },
  failed: {
    icon: CircleX,
    tone: 'bg-destructive/10 text-destructive',
  },
  slow: {
    icon: Clock,
    tone: 'bg-muted text-muted-foreground',
  },
  offline: {
    icon: WifiOff,
    tone: 'bg-destructive/10 text-destructive',
  },
  unknown: {
    icon: CircleAlert,
    tone: 'bg-destructive/10 text-destructive',
  },
}

export function PaymentResultPage() {
  const { t } = useTranslation('payment')
  const [searchParams] = useSearchParams()
  const tx = searchParams.get('tx') ?? ''
  const [startedAt] = useState(() => Date.now())
  const [gaveUp, setGaveUp] = useState(false)

  const status = usePaymentStatusQuery(tx, { skip: !tx })
  const pending = status.data?.status === 'PENDING'

  // Poll while the payment is still pending, and stop after a while.
  useEffect(() => {
    if (!pending || gaveUp) return
    const timer = setTimeout(() => {
      if (Date.now() - startedAt > GIVE_UP_MS) setGaveUp(true)
      else void status.refetch()
    }, POLL_MS)
    return () => clearTimeout(timer)
  }, [pending, gaveUp, startedAt, status])

  let screen: Screen
  if (!tx) screen = 'unknown'
  else if (status.error && 'status' in status.error) {
    screen =
      status.error.status === 'FETCH_ERROR'
        ? 'offline'
        : status.error.status === 404
          ? 'unknown'
          : 'offline'
  } else if (!status.data) screen = 'checking'
  else if (status.data.status === 'SUCCESS') screen = 'success'
  else if (status.data.status === 'FAILED' || status.data.status === 'REFUNDED')
    screen = 'failed'
  else screen = gaveUp ? 'slow' : 'checking'

  const view = SCREENS[screen]
  const Icon = view.icon
  const data = status.data
  const tableLink = data
    ? `/r/${data.restaurantId}/t/${data.tableId}?token=${data.qrToken}`
    : null

  return (
    <main className="flex min-h-svh items-center justify-center p-4 sm:p-6">
      <Card className="w-full max-w-md" aria-live="polite">
        <CardHeader className="items-center text-center">
          <div
            className={cn(
              'flex size-16 items-center justify-center rounded-full',
              view.tone,
            )}
          >
            {screen === 'checking' ? (
              <LoaderCircle className="size-8 animate-spin" aria-hidden />
            ) : (
              <Icon className="size-8" aria-hidden />
            )}
          </div>
          <CardTitle>{t(`result.${screen}.title`)}</CardTitle>
          <CardDescription>{t(`result.${screen}.text`)}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {data && (
            <div className="flex items-center justify-between rounded-lg bg-muted px-4 py-3 text-sm">
              <span className="text-muted-foreground">
                {t('result.amount')}
              </span>
              <span className="font-semibold tabular-nums">
                {formatCurrency(data.amount, data.currency)}
              </span>
            </div>
          )}
          {(screen === 'offline' || screen === 'slow') && (
            <Button
              className="w-full"
              onClick={() => {
                setGaveUp(false)
                void status.refetch()
              }}
            >
              <RotateCw aria-hidden />
              {t('result.retry')}
            </Button>
          )}
          {tableLink && screen !== 'checking' && (
            <Button
              variant={screen === 'success' ? 'default' : 'outline'}
              className="w-full"
              asChild
            >
              <Link to={tableLink}>{t('result.back')}</Link>
            </Button>
          )}
        </CardContent>
      </Card>
    </main>
  )
}
