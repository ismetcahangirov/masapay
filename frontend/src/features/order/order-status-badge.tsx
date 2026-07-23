import type { LucideIcon } from 'lucide-react'
import {
  CircleDollarSign,
  Clock,
  ReceiptText,
  CheckCircle2,
} from 'lucide-react'
import type { OrderStatus } from '@/features/order/types'
import { Badge, type BadgeProps } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

// Status is communicated with colour + a lucide icon only (no emoji), per the
// project design rules. Each status maps to one Badge variant and one icon.
const STATUS_CONFIG: Record<
  OrderStatus,
  { label: string; variant: BadgeProps['variant']; icon: LucideIcon }
> = {
  open: { label: 'Açıq', variant: 'secondary', icon: ReceiptText },
  awaiting_payment: {
    label: 'Ödəniş gözlənilir',
    variant: 'default',
    icon: Clock,
  },
  partially_paid: {
    label: 'Qismən ödənilib',
    variant: 'outline',
    icon: CircleDollarSign,
  },
  paid: { label: 'Ödənilib', variant: 'success', icon: CheckCircle2 },
}

export function OrderStatusBadge({
  status,
  className,
}: {
  status: OrderStatus
  className?: string
}) {
  const { label, variant, icon: Icon } = STATUS_CONFIG[status]

  return (
    <Badge variant={variant} className={cn('gap-1', className)}>
      <Icon className="size-3.5" aria-hidden />
      {label}
    </Badge>
  )
}
