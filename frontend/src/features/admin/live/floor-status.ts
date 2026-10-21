import {
  CircleCheck,
  CircleDashed,
  Coins,
  type LucideIcon,
  UtensilsCrossed,
} from 'lucide-react'
import type { FloorStatus } from '@/features/admin/live/floor-api'

interface StatusStyle {
  label: string
  icon: LucideIcon
  /** Tile surface: border, background and text colour tokens. */
  tile: string
  /** Small status pill in the tile's corner and in the legend. */
  pill: string
}

/** Display order of the statuses (legend and counts). */
export const FLOOR_STATUSES: FloorStatus[] = [
  'EMPTY',
  'ACTIVE',
  'PARTIALLY_PAID',
  'CLOSED',
]

// Each status has its own icon, colour and label, so it reads at a glance
// and still without colour (icon + text).
export const FLOOR_STATUS: Record<FloorStatus, StatusStyle> = {
  EMPTY: {
    label: 'Boş',
    icon: CircleDashed,
    tile: 'border-dashed bg-card text-muted-foreground',
    pill: 'bg-muted text-muted-foreground',
  },
  ACTIVE: {
    label: 'Aktiv',
    icon: UtensilsCrossed,
    tile: 'border-primary bg-card text-foreground',
    pill: 'bg-primary text-primary-foreground',
  },
  PARTIALLY_PAID: {
    label: 'Qismən ödənilib',
    icon: Coins,
    tile: 'border-success bg-success/10 text-foreground',
    pill: 'border border-success bg-card text-foreground',
  },
  CLOSED: {
    label: 'Bağlı',
    icon: CircleCheck,
    tile: 'border-success bg-success text-success-foreground',
    pill: 'bg-card text-foreground',
  },
}

/** "25 dəq" or "1 saat 5 dəq". */
export function formatMinutes(minutes: number): string {
  if (minutes < 60) return `${minutes} dəq`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest === 0 ? `${hours} saat` : `${hours} saat ${rest} dəq`
}
