import { ChefHat, CircleCheck, Clock } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import type { KitchenStatus } from '@/features/order/types'
import { cn } from '@/lib/utils'

const ICONS = { NEW: Clock, PREPARING: ChefHat, READY: CircleCheck }

/**
 * Where a dish is in the kitchen (#142, #178), the same for the customer and
 * the staff. Lines that never go to the kitchen have no status and no badge.
 */
export function KitchenStatusBadge({
  status,
  className,
}: {
  status: KitchenStatus
  className?: string
}) {
  const { t } = useTranslation('customer')
  const Icon = ICONS[status]
  return (
    <Badge
      variant={status === 'READY' ? 'success' : 'outline'}
      className={cn('shrink-0 gap-1 font-medium', className)}
    >
      <Icon className="size-3" aria-hidden />
      {t(`kitchenStatus.${status}`)}
    </Badge>
  )
}
