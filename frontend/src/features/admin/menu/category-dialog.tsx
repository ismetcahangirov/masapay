import { type FormEvent, useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { isConflict } from './menu-errors'
import {
  type MenuCategory,
  useCreateCategoryMutation,
  useUpdateCategoryMutation,
} from './menu-api'

// Adds a category or renames / reorders an existing one (#137).
export function CategoryDialog({
  restaurantId,
  open,
  category,
  nextSortOrder,
  onClose,
}: {
  restaurantId: string
  open: boolean
  /** The category to edit; null adds a new one. */
  category: MenuCategory | null
  /** Where a new category goes: after the last one. */
  nextSortOrder: number
  onClose: () => void
}) {
  const [name, setName] = useState('')
  const [sortOrder, setSortOrder] = useState('0')
  const [create, createState] = useCreateCategoryMutation()
  const [update, updateState] = useUpdateCategoryMutation()
  const state = category ? updateState : createState

  // Fill the form each time the dialog opens.
  useEffect(() => {
    if (!open) return
    setName(category?.name ?? '')
    setSortOrder(String(category?.sortOrder ?? nextSortOrder))
    createState.reset()
    updateState.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, category, nextSortOrder])

  const order = Number(sortOrder)
  const valid = name.trim() !== '' && Number.isInteger(order) && order >= 0

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!valid) return
    const body = { name: name.trim(), sortOrder: order }
    try {
      if (category) {
        await update({
          restaurantId,
          categoryId: category.id,
          body,
        }).unwrap()
      } else {
        await create({ restaurantId, body }).unwrap()
      }
      onClose()
    } catch {
      // state.error is shown below.
    }
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="max-w-md">
        <form onSubmit={(event) => void onSubmit(event)} className="space-y-4">
          <DialogHeader>
            <DialogTitle>
              {category ? 'Kateqoriyanı redaktə et' : 'Yeni kateqoriya'}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="category-name">Ad</Label>
            <Input
              id="category-name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="Məsələn, Salatlar"
              maxLength={100}
              autoFocus
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="category-order">Sıra</Label>
            <Input
              id="category-order"
              type="number"
              min={0}
              step={1}
              value={sortOrder}
              onChange={(event) => setSortOrder(event.target.value)}
            />
            <p className="text-xs text-muted-foreground">
              Kiçik rəqəm menyuda yuxarıda göstərilir.
            </p>
          </div>
          {state.isError && (
            <p className="text-sm text-destructive">
              {isConflict(state.error)
                ? 'Bu adda kateqoriya artıq var.'
                : 'Yadda saxlamaq alınmadı.'}
            </p>
          )}
          <DialogFooter className="gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              İmtina
            </Button>
            <Button type="submit" disabled={!valid || state.isLoading}>
              {state.isLoading ? 'Yadda saxlanılır...' : 'Yadda saxla'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
