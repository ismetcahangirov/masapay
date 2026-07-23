import { useId, useState } from 'react'
import { TriangleAlert } from 'lucide-react'
import {
  TIP_PERCENT_PRESETS,
  parseTipInput,
  tipFromPercent,
} from '@/features/tip/tip'
import { formatCurrency } from '@/features/order/format'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

const CUSTOM_MODE = 'custom'

// Only digits, one separator and up to two decimals may be typed. This blocks
// "-", letters and extra decimals at the keystroke level; parseTipInput is the
// authoritative guard for the emitted amount.
const KEYSTROKE_RE = /^\d*(?:[.,]\d{0,2})?$/

export interface TipSelectorProps {
  /** Bill amount the percentage presets apply to, in minor units. */
  baseMinor: number
  /** ISO-4217 currency code, e.g. "AZN". */
  currency: string
  /** Called with the selected tip in minor units whenever it changes. */
  onChange?: (tipMinor: number) => void
}

export function TipSelector({
  baseMinor,
  currency,
  onChange,
}: TipSelectorProps) {
  const [mode, setMode] = useState<string>('0')
  const [customText, setCustomText] = useState('')
  const inputId = useId()

  const isCustom = mode === CUSTOM_MODE
  const customParsed = parseTipInput(customText)
  const customInvalid =
    isCustom && customText.trim() !== '' && customParsed === null

  const tipMinor = isCustom
    ? (customParsed ?? 0)
    : tipFromPercent(baseMinor, Number(mode))

  function emit(nextTip: number) {
    onChange?.(nextTip)
  }

  function handleModeChange(next: string) {
    // ToggleGroup emits "" when the active item is toggled off; keep selection.
    if (!next) return
    setMode(next)
    if (next === CUSTOM_MODE) {
      emit(parseTipInput(customText) ?? 0)
    } else {
      emit(tipFromPercent(baseMinor, Number(next)))
    }
  }

  function handleCustomChange(raw: string) {
    if (!KEYSTROKE_RE.test(raw)) return
    setCustomText(raw)
    emit(parseTipInput(raw) ?? 0)
  }

  return (
    <div className="space-y-3">
      <ToggleGroup
        type="single"
        value={mode}
        onValueChange={handleModeChange}
        variant="outline"
        className="flex-wrap justify-start"
      >
        {TIP_PERCENT_PRESETS.map((percent) => (
          <ToggleGroupItem key={percent} value={String(percent)}>
            {percent}%
          </ToggleGroupItem>
        ))}
        <ToggleGroupItem value={CUSTOM_MODE}>Fərqli məbləğ</ToggleGroupItem>
      </ToggleGroup>

      {isCustom && (
        <div className="space-y-1.5">
          <Label htmlFor={inputId}>Bəxşiş məbləği (AZN)</Label>
          <Input
            id={inputId}
            inputMode="decimal"
            placeholder="0,00"
            value={customText}
            onChange={(event) => handleCustomChange(event.target.value)}
            aria-invalid={customInvalid}
            aria-describedby={customInvalid ? `${inputId}-error` : undefined}
          />
          {customInvalid && (
            <p
              id={`${inputId}-error`}
              className="flex items-center gap-1 text-sm text-destructive"
            >
              <TriangleAlert className="size-3.5" aria-hidden />
              Düzgün məbləğ daxil edin.
            </p>
          )}
        </div>
      )}

      <p className="text-sm text-muted-foreground">
        Bəxşiş:{' '}
        <span className="font-medium text-foreground">
          {formatCurrency(tipMinor, currency)}
        </span>
      </p>
    </div>
  )
}
