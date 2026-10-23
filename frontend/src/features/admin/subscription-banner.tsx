import { CalendarClock, CircleAlert } from 'lucide-react'
import { useAppSelector } from '@/app/hooks'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import {
  WARN_DAYS,
  bakuToday,
  daysBetween,
  formatDay,
} from '@/features/admin/platform/subscription'

// Tells the manager when their restaurant's subscription has ended, or ends
// within a week (#148). After the last day customers can still order, but
// cannot pay online.
export function SubscriptionBanner() {
  const role = useAppSelector((state) => state.auth.user?.role)
  const restaurants = useMyRestaurantsQuery(undefined, {
    skip: role !== 'RESTO_MANAGER',
  })
  const restaurant = restaurants.data?.[0]
  if (role !== 'RESTO_MANAGER' || !restaurant?.subscriptionEndsOn) return null

  const endsOn = formatDay(restaurant.subscriptionEndsOn)
  if (restaurant.subscriptionStatus === 'EXPIRED') {
    return (
      <div
        role="alert"
        className="flex items-start gap-3 rounded-lg border border-destructive bg-destructive/10 p-4 text-sm"
      >
        <CircleAlert
          className="mt-0.5 size-5 shrink-0 text-destructive"
          aria-hidden
        />
        <p>
          <span className="font-semibold">Abunəlik bitib</span> (son gün{' '}
          {endsOn}). Müştərilər onlayn ödəyə bilmir, sifariş vermək isə açıqdır.
          Uzatmaq üçün masapay ilə əlaqə saxlayın.
        </p>
      </div>
    )
  }

  const daysLeft = daysBetween(bakuToday(), restaurant.subscriptionEndsOn)
  if (daysLeft > WARN_DAYS) return null
  return (
    <div
      role="status"
      className="flex items-start gap-3 rounded-lg border border-foreground bg-muted p-4 text-sm"
    >
      <CalendarClock className="mt-0.5 size-5 shrink-0" aria-hidden />
      <p>
        <span className="font-semibold">
          {daysLeft === 0
            ? 'Abunəliyin bu gün son günüdür'
            : `Abunəliyin bitməsinə ${daysLeft} gün qalıb`}
        </span>{' '}
        (son gün {endsOn}). Bitəndən sonra müştərilər onlayn ödəyə bilməyəcək.
      </p>
    </div>
  )
}
