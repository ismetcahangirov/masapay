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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import {
  type MenuCategory,
  type MenuDish,
  useCreateDishMutation,
  useUpdateDishMutation,
} from './menu-api'
import { parsePrice } from './menu-price'

// Adds a dish to a category or edits one, including moving it to another
// category (#137).
export function DishDialog({
  restaurantId,
  open,
  dish,
  categories,
  defaultCategoryId,
  onClose,
}: {
  restaurantId: string
  open: boolean
  /** The dish to edit; null adds a new one. */
  dish: MenuDish | null
  categories: MenuCategory[]
  /** The category a new dish starts in. */
  defaultCategoryId: string | null
  onClose: () => void
}) {
  const [categoryId, setCategoryId] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [price, setPrice] = useState('')
  const [create, createState] = useCreateDishMutation()
  const [update, updateState] = useUpdateDishMutation()
  const state = dish ? updateState : createState

  useEffect(() => {
    if (!open) return
    setCategoryId(dish?.categoryId ?? defaultCategoryId ?? '')
    setName(dish?.name ?? '')
    setDescription(dish?.description ?? '')
    setPrice(dish ? dish.price.toFixed(2) : '')
    createState.reset()
    updateState.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, dish, defaultCategoryId])

  const parsedPrice = parsePrice(price)
  const valid = categoryId !== '' && name.trim() !== '' && parsedPrice !== null

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!valid || parsedPrice === null) return
    const body = {
      categoryId,
      name: name.trim(),
      description: description.trim() || null,
      price: parsedPrice,
    }
    try {
      if (dish) {
        await update({ restaurantId, dishId: dish.id, body }).unwrap()
      } else {
        await create({ restaurantId, body }).unwrap()
      }
      onClose()
    } catch {
      // state.isError is shown below.
    }
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="max-w-md">
        <form onSubmit={(event) => void onSubmit(event)} className="space-y-4">
          <DialogHeader>
            <DialogTitle>
              {dish ? 'Məhsulu redaktə et' : 'Yeni məhsul'}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="dish-category">Kateqoriya</Label>
            <Select value={categoryId} onValueChange={setCategoryId}>
              <SelectTrigger id="dish-category">
                <SelectValue placeholder="Kateqoriya seçin" />
              </SelectTrigger>
              <SelectContent>
                {categories.map((category) => (
                  <SelectItem key={category.id} value={category.id}>
                    {category.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="dish-name">Ad</Label>
            <Input
              id="dish-name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="Məsələn, Sezar salatı"
              maxLength={150}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="dish-description">Təsvir (istəyə bağlı)</Label>
            <Textarea
              id="dish-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Tərkibi, porsiya və s."
              maxLength={500}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="dish-price">Qiymət (AZN)</Label>
            <Input
              id="dish-price"
              inputMode="decimal"
              value={price}
              onChange={(event) => setPrice(event.target.value)}
              placeholder="0.00"
              aria-invalid={price !== '' && parsedPrice === null}
            />
            {price !== '' && parsedPrice === null && (
              <p className="text-xs text-destructive">
                Qiyməti rəqəmlə yazın, ən çox 2 onluq: məsələn 8.50
              </p>
            )}
          </div>
          {state.isError && (
            <p className="text-sm text-destructive">Yadda saxlamaq alınmadı.</p>
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
