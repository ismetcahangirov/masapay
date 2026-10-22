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
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { type AdminTable, useSetTableServiceChargeMutation } from './tables-api'
import { parsePercent } from './service-charge'

type Mode = 'RESTAURANT' | 'OWN' | 'NONE'

function modeOf(table: AdminTable): Mode {
  if (table.serviceChargePercent === null) return 'RESTAURANT'
  return table.serviceChargePercent === 0 ? 'NONE' : 'OWN'
}

// A table's own service charge (#146): follow the restaurant, its own
// percentage (e.g. 15% in the VIP room) or none at this table.
export function ServiceChargeDialog({
  restaurantId,
  table,
  onClose,
}: {
  restaurantId: string
  /** The table to change; null keeps the dialog closed. */
  table: AdminTable | null
  onClose: () => void
}) {
  const [mode, setMode] = useState<Mode>('RESTAURANT')
  const [percent, setPercent] = useState('')
  const [save, saveState] = useSetTableServiceChargeMutation()

  useEffect(() => {
    if (!table) return
    setMode(modeOf(table))
    setPercent(
      table.serviceChargePercent ? String(table.serviceChargePercent) : '',
    )
    saveState.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [table])

  const parsed = parsePercent(percent)
  const valid = mode !== 'OWN' || parsed !== null

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!table || !valid) return
    const value = mode === 'RESTAURANT' ? null : mode === 'NONE' ? 0 : parsed
    try {
      await save({ restaurantId, tableId: table.id, percent: value }).unwrap()
      onClose()
    } catch {
      // saveState.isError is shown below.
    }
  }

  return (
    <Dialog open={table !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-md">
        <form onSubmit={(event) => void onSubmit(event)} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Xidmət haqqı</DialogTitle>
            <DialogDescription>
              {table?.label}: bu masanın hesablarına nə qədər xidmət haqqı
              gəlsin. Yeni sətir əlavə olunanda tətbiq olunur.
            </DialogDescription>
          </DialogHeader>
          <ToggleGroup
            type="single"
            value={mode}
            onValueChange={(value) => value && setMode(value as Mode)}
            className="grid grid-cols-3 gap-2"
            aria-label="Xidmət haqqı"
          >
            <ToggleGroupItem value="RESTAURANT" className="h-auto py-2">
              Restoranınkı
            </ToggleGroupItem>
            <ToggleGroupItem value="OWN" className="h-auto py-2">
              Xüsusi faiz
            </ToggleGroupItem>
            <ToggleGroupItem value="NONE" className="h-auto py-2">
              Yoxdur
            </ToggleGroupItem>
          </ToggleGroup>
          {mode === 'OWN' && (
            <div className="space-y-1">
              <Label htmlFor="table-service-charge">Faiz (%)</Label>
              <Input
                id="table-service-charge"
                inputMode="decimal"
                value={percent}
                onChange={(event) => setPercent(event.target.value)}
                placeholder="15"
                aria-invalid={percent !== '' && parsed === null}
              />
              {percent !== '' && parsed === null && (
                <p className="text-xs text-destructive">
                  0-dan böyük, 100-dən kiçik rəqəm yazın, məsələn 15 və ya 12,5
                </p>
              )}
            </div>
          )}
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
