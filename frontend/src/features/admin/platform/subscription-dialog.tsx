import { type FormEvent, useEffect, useState } from 'react'
import { CalendarPlus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import {
  type PlatformRestaurant,
  type SubscriptionPlan,
  useSetSubscriptionMutation,
} from './platform-api'
import { addMonths, bakuToday } from './subscription'

const DAY = /^\d{4}-\d{2}-\d{2}$/

function parseFee(value: string): number | null {
  const fee = Number(value.trim().replace(',', '.'))
  return value.trim() !== '' && Number.isFinite(fee) && fee >= 0 ? fee : null
}

// A restaurant's subscription (#148): plan, trial or paid, first and last day
// and the monthly fee. Extending is a later last day; "+1 ay" adds a month to
// the last day (or to today, once it has already passed).
export function SubscriptionDialog({
  restaurant,
  onClose,
}: {
  /** The restaurant to change; null keeps the dialog closed. */
  restaurant: PlatformRestaurant | null
  onClose: () => void
}) {
  const [plan, setPlan] = useState<SubscriptionPlan>('BASIC')
  const [trial, setTrial] = useState(false)
  const [startsOn, setStartsOn] = useState('')
  const [endsOn, setEndsOn] = useState('')
  const [fee, setFee] = useState('')
  const [save, saveState] = useSetSubscriptionMutation()

  useEffect(() => {
    if (!restaurant) return
    const current = restaurant.subscription
    const today = bakuToday()
    setPlan(current.plan ?? 'BASIC')
    setTrial(current.trial)
    setStartsOn(current.startsOn ?? today)
    setEndsOn(current.endsOn ?? addMonths(today, 1))
    setFee(current.monthlyFee != null ? String(current.monthlyFee) : '')
    saveState.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [restaurant])

  const monthlyFee = parseFee(fee)
  const backwards = DAY.test(startsOn) && DAY.test(endsOn) && endsOn < startsOn
  const valid =
    DAY.test(startsOn) && DAY.test(endsOn) && !backwards && monthlyFee !== null

  function extendByMonth() {
    const today = bakuToday()
    const from = DAY.test(endsOn) && endsOn >= today ? endsOn : today
    setEndsOn(addMonths(from, 1))
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!restaurant || !valid || monthlyFee === null) return
    try {
      await save({
        id: restaurant.id,
        plan,
        trial,
        startsOn,
        endsOn,
        monthlyFee,
      }).unwrap()
      onClose()
    } catch {
      // saveState.isError is shown below.
    }
  }

  return (
    <Dialog
      open={restaurant !== null}
      onOpenChange={(open) => !open && onClose()}
    >
      <DialogContent className="max-w-md">
        <form onSubmit={(event) => void onSubmit(event)} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Abunəlik</DialogTitle>
            <DialogDescription>
              {restaurant?.name}: son gündən sonra müştərilər onlayn ödəyə
              bilmir, sifariş vermək isə açıq qalır.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1">
            <Label id="subscription-plan">Plan</Label>
            <ToggleGroup
              type="single"
              value={plan}
              onValueChange={(value) =>
                value && setPlan(value as SubscriptionPlan)
              }
              className="grid grid-cols-2 gap-2"
              aria-labelledby="subscription-plan"
            >
              <ToggleGroupItem value="BASIC">Basic</ToggleGroupItem>
              <ToggleGroupItem value="PRO">Pro</ToggleGroupItem>
            </ToggleGroup>
          </div>
          <div className="space-y-1">
            <Label id="subscription-kind">Növ</Label>
            <ToggleGroup
              type="single"
              value={trial ? 'TRIAL' : 'PAID'}
              onValueChange={(value) => value && setTrial(value === 'TRIAL')}
              className="grid grid-cols-2 gap-2"
              aria-labelledby="subscription-kind"
            >
              <ToggleGroupItem value="PAID">Ödənişli</ToggleGroupItem>
              <ToggleGroupItem value="TRIAL">Sınaq</ToggleGroupItem>
            </ToggleGroup>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label htmlFor="subscription-starts">Başlama günü</Label>
              <Input
                id="subscription-starts"
                type="date"
                value={startsOn}
                onChange={(event) => setStartsOn(event.target.value)}
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="subscription-ends">Son gün</Label>
              <Input
                id="subscription-ends"
                type="date"
                value={endsOn}
                onChange={(event) => setEndsOn(event.target.value)}
                aria-invalid={backwards}
              />
            </div>
          </div>
          {backwards && (
            <p className="text-xs text-destructive">
              Son gün başlama günündən əvvəl ola bilməz.
            </p>
          )}
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={extendByMonth}
          >
            <CalendarPlus aria-hidden />
            +1 ay
          </Button>
          <div className="space-y-1">
            <Label htmlFor="subscription-fee">
              Aylıq ödəniş ({restaurant?.currency})
            </Label>
            <Input
              id="subscription-fee"
              inputMode="decimal"
              value={fee}
              onChange={(event) => setFee(event.target.value)}
              placeholder="49.00"
              aria-invalid={fee !== '' && monthlyFee === null}
            />
          </div>
          {saveState.isError && (
            <p className="text-sm text-destructive">Yadda saxlamaq alınmadı.</p>
          )}
          <DialogFooter className="gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              İmtina
            </Button>
            <Button type="submit" disabled={!valid || saveState.isLoading}>
              {saveState.isLoading ? 'Yadda saxlanılır...' : 'Yadda saxla'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
