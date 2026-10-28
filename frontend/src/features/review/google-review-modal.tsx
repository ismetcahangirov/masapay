import { useState } from 'react'
import { Star } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import type { PaymentStatus } from '@/features/payment/types'
import {
  buildGoogleReviewUrl,
  shouldShowReviewModal,
} from '@/features/review/google-review'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

export interface GoogleReviewModalProps {
  /** Current payment status; the modal only opens when this is APPROVED. */
  paymentStatus: PaymentStatus
  /** Restaurant Google Place ID the review link points to. */
  placeId: string
  /** Restaurant name shown in the prompt. */
  restaurantName?: string
}

function FiveStars() {
  return (
    <div className="flex justify-center gap-1 text-success" aria-hidden>
      {Array.from({ length: 5 }, (_, index) => (
        <Star key={index} className="size-7 fill-current" />
      ))}
    </div>
  )
}

// Post-payment prompt inviting the guest to leave a 5-star Google review.
// Rendered only when the payment is APPROVED (acceptance criterion 6.3); the
// guest can dismiss it.
export function GoogleReviewModal({
  paymentStatus,
  placeId,
  restaurantName,
}: GoogleReviewModalProps) {
  const { t } = useTranslation('payment')
  const [dismissed, setDismissed] = useState(false)
  const open = shouldShowReviewModal(paymentStatus) && !dismissed

  return (
    <Dialog open={open} onOpenChange={(next) => !next && setDismissed(true)}>
      <DialogContent>
        <DialogHeader>
          <FiveStars />
          <DialogTitle className="text-center">{t('review.title')}</DialogTitle>
          <DialogDescription className="text-center">
            {restaurantName
              ? t('review.textNamed', { name: restaurantName })
              : t('review.text')}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="gap-2 sm:flex-col">
          <Button variant="success" asChild>
            <a
              href={buildGoogleReviewUrl(placeId)}
              target="_blank"
              rel="noopener noreferrer"
              onClick={() => setDismissed(true)}
            >
              <Star />
              {t('review.rate')}
            </a>
          </Button>
          <Button variant="ghost" onClick={() => setDismissed(true)}>
            {t('review.later')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
