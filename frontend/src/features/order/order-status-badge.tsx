import type { LucideIcon } from 'lucide-react'
import {
  Ban,
  CircleDollarSign,
  Clock,
  ReceiptText,
  CheckCircle2,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import type { OrderStatus } from '@/features/order/types'
import { Badge, type BadgeProps } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

// Status is communicated with colour + a lucide icon only (no emoji), per the
// project design rules. Each status maps to one Badge variant and one icon;
// its label is the translation orderStatus.<status>.
const STATUS_CONFIG: Record<
  OrderStatus,
  { variant: BadgeProps['variant']; icon: LucideIcon }
> = {
  open: { variant: 'secondary', icon: ReceiptText },
  awaiting_payment: { variant: 'default', icon: Clock },
  partially_paid: { variant: 'outline', icon: CircleDollarSign },
  paid: { variant: 'success', icon: CheckCircle2 },
  cancelled: { variant: 'destructive', icon: Ban },
}

export function OrderStatusBadge({
  status,
  className,
}: {
  status: OrderStatus
  className?: string
}) {
  const { t } = useTranslation('customer')
  const { variant, icon: Icon } = STATUS_CONFIG[status]

  return (
    <Badge variant={variant} className={cn('gap-1', className)}>
      <Icon className="size-3.5" aria-hidden />
      {t(`orderStatus.${status}`)}
    </Badge>
  )
}
