import { type FormEvent, useEffect, useState } from 'react'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { currentMode } from './availability'
import {
  type AvailabilityMode,
  type MenuDish,
  useSetAvailabilityMutation,
} from './menu-api'

const MODES: Record<AvailabilityMode, string> = {
  ALWAYS: 'Həmişə menyuda',
  OFF_TODAY: 'Yalnız bu gün söndür',
  DATES: 'Yalnız bu tarixlərdə',
  OFF: 'Söndür (yenidən açana qədər)',
}

// Puts a dish on or off the daily menu (#139): always, off for today only,
// only between two days, or off until switched back on.
export function AvailabilityDialog({
  restaurantId,
  dish,
  onClose,
}: {
  restaurantId: string
  /** The dish to change; null keeps the dialog closed. */
  dish: MenuDish | null
  onClose: () => void
}) {
  const [mode, setMode] = useState<AvailabilityMode>('ALWAYS')
  const [from, setFrom] = useState('')
  const [until, setUntil] = useState('')
  const [save, saveState] = useSetAvailabilityMutation()

  useEffect(() => {
    if (!dish) return
    setMode(currentMode(dish))
    setFrom(dish.availableFrom ?? '')
    setUntil(dish.availableUntil ?? '')
    saveState.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dish])

  const valid =
    mode !== 'DATES' ||
    ((from !== '' || until !== '') && (!from || !until || from <= until))

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!dish || !valid) return
    const dates = mode === 'DATES'
    try {
      await save({
        restaurantId,
        dishId: dish.id,
        body: {
          mode,
          from: dates && from ? from : null,
          until: dates && until ? until : null,
        },
      }).unwrap()
      onClose()
    } catch {
      // saveState.isError is shown below.
    }
  }

  return (
    <Dialog open={dish !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-md">
        <form onSubmit={(event) => void onSubmit(event)} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Günlük menyu</DialogTitle>
            <DialogDescription>
              {dish?.name}: müştəri onu nə vaxt görə və sifariş edə bilər.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="availability-mode">Mövcudluq</Label>
            <Select
              value={mode}
              onValueChange={(value) => setMode(value as AvailabilityMode)}
            >
              <SelectTrigger id="availability-mode">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {(Object.keys(MODES) as AvailabilityMode[]).map((value) => (
                  <SelectItem key={value} value={value}>
                    {MODES[value]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {mode === 'OFF_TODAY' && (
              <p className="text-xs text-muted-foreground">
                Sabah özü yenidən menyuya qayıdacaq.
              </p>
            )}
          </div>
          {mode === 'DATES' && (
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label htmlFor="availability-from">Başlanğıc</Label>
                <Input
                  id="availability-from"
                  type="date"
                  value={from}
                  max={until || undefined}
                  onChange={(event) => setFrom(event.target.value)}
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="availability-until">Son gün</Label>
                <Input
                  id="availability-until"
                  type="date"
                  value={until}
                  min={from || undefined}
                  onChange={(event) => setUntil(event.target.value)}
                />
              </div>
              <p className="col-span-2 text-xs text-muted-foreground">
                Hər iki gün daxildir. Biri boş qalsa, o tərəfdən məhdudiyyət
                yoxdur.
              </p>
            </div>
          )}
          {saveState.isError && (
            <p className="text-sm text-destructive">
              Yadda saxlamaq alınmadı. Tarixləri yoxlayın.
            </p>
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
