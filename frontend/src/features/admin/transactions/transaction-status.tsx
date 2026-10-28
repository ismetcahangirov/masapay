import { useTranslation } from 'react-i18next'
import { CircleCheck, CircleX, Clock, Undo2 } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { TransactionStatus } from './transactions-api'

const STATUS_STYLES: Record<
  TransactionStatus,
  { icon: typeof CircleCheck; className: string }
> = {
  PENDING: { icon: Clock, className: 'bg-muted text-muted-foreground' },
  SUCCESS: {
    icon: CircleCheck,
    className: 'bg-success text-success-foreground',
  },
  FAILED: {
    icon: CircleX,
    className: 'bg-destructive text-destructive-foreground',
  },
  REFUNDED: {
    icon: Undo2,
    className: 'bg-secondary text-secondary-foreground',
  },
}

/** A payment's state as a small pill: icon, colour token and translated label. */
export function TransactionStatusBadge({
  status,
}: {
  status: TransactionStatus
}) {
  const { t } = useTranslation('transactions')
  const { icon: Icon, className } = STATUS_STYLES[status]
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold',
        className,
      )}
    >
      <Icon className="size-3.5" aria-hidden />
      {t(`status.${status}`)}
    </span>
  )
}
