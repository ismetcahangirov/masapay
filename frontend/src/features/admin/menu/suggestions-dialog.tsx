import { useEffect, useState } from 'react'
import { Check, Plus } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  type MenuCategory,
  type MenuDish,
  useSetSuggestionsMutation,
} from './menu-api'

export const MAX_SUGGESTIONS = 5

// Which extras (a sauce, a drink) are offered with a dish before the customer
// confirms (#141). They are offered in the order they are picked here.
export function SuggestionsDialog({
  restaurantId,
  dish,
  categories,
  onClose,
}: {
  restaurantId: string
  /** The dish to set extras for; null keeps the dialog closed. */
  dish: MenuDish | null
  categories: MenuCategory[]
  onClose: () => void
}) {
  const { t } = useTranslation('menu')
  const [picked, setPicked] = useState<string[]>([])
  const [save, saveState] = useSetSuggestionsMutation()

  useEffect(() => {
    if (!dish) return
    setPicked(dish.suggestionIds)
    saveState.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dish])

  const others = categories.flatMap((category) =>
    category.items.filter((item) => item.id !== dish?.id),
  )
  const nameOf = new Map(others.map((item) => [item.id, item.name]))

  function toggle(id: string) {
    setPicked((current) =>
      current.includes(id)
        ? current.filter((x) => x !== id)
        : current.length < MAX_SUGGESTIONS
          ? [...current, id]
          : current,
    )
  }

  async function onSave() {
    if (!dish) return
    try {
      await save({ restaurantId, dishId: dish.id, itemIds: picked }).unwrap()
      onClose()
    } catch {
      // saveState.isError is shown below.
    }
  }

  return (
    <Dialog open={dish !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{t('suggestionsDialog.title')}</DialogTitle>
          <DialogDescription>
            {t('suggestionsDialog.description', {
              name: dish?.name ?? '',
              max: MAX_SUGGESTIONS,
            })}
          </DialogDescription>
        </DialogHeader>
        {picked.length > 0 && (
          <ol
            className="list-inside list-decimal text-sm"
            aria-label={t('suggestionsDialog.picked')}
          >
            {picked.map((id) => (
              <li key={id}>{nameOf.get(id) ?? id}</li>
            ))}
          </ol>
        )}
        <ul className="max-h-64 divide-y overflow-y-auto rounded-md border">
          {others.map((item) => {
            const on = picked.includes(item.id)
            return (
              <li
                key={item.id}
                className="flex items-center justify-between gap-2 px-3 py-2"
              >
                <span className={on ? 'text-sm font-medium' : 'text-sm'}>
                  {item.name}
                </span>
                <Button
                  type="button"
                  variant={on ? 'secondary' : 'outline'}
                  size="sm"
                  aria-pressed={on}
                  disabled={!on && picked.length >= MAX_SUGGESTIONS}
                  onClick={() => toggle(item.id)}
                >
                  {on ? <Check aria-hidden /> : <Plus aria-hidden />}
                  {on
                    ? t('suggestionsDialog.selected')
                    : t('suggestionsDialog.offer')}
                </Button>
              </li>
            )
          })}
        </ul>
        {saveState.isError && (
          <p className="text-sm text-destructive">{t('actions.saveFailed')}</p>
        )}
        <DialogFooter className="gap-2">
          <Button type="button" variant="outline" onClick={onClose}>
            {t('actions.cancel')}
          </Button>
          <Button
            type="button"
            onClick={() => void onSave()}
            disabled={saveState.isLoading}
          >
            {saveState.isLoading ? t('actions.saving') : t('actions.save')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
