import { type FormEvent, useEffect, useState } from 'react'
import { Minus, Plus } from 'lucide-react'
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

const MAX_QUANTITY = 99

// Adds a dish to a category or edits one, including moving it to another
// category (#137). In combo mode it also picks the dishes a combo or business
// lunch is made of, with a quantity each; the combo has its own price (#138).
export function DishDialog({
  restaurantId,
  open,
  dish,
  combo,
  categories,
  defaultCategoryId,
  onClose,
}: {
  restaurantId: string
  open: boolean
  /** The dish to edit; null adds a new one. */
  dish: MenuDish | null
  /** True to edit a combo's contents as well. */
  combo: boolean
  categories: MenuCategory[]
  /** The category a new dish starts in. */
  defaultCategoryId: string | null
  onClose: () => void
}) {
  const [categoryId, setCategoryId] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [price, setPrice] = useState('')
  const [quantities, setQuantities] = useState<Record<string, number>>({})
  const [create, createState] = useCreateDishMutation()
  const [update, updateState] = useUpdateDishMutation()
  const state = dish ? updateState : createState

  useEffect(() => {
    if (!open) return
    setCategoryId(dish?.categoryId ?? defaultCategoryId ?? '')
    setName(dish?.name ?? '')
    setDescription(dish?.description ?? '')
    setPrice(dish ? dish.price.toFixed(2) : '')
    setQuantities(
      Object.fromEntries(
        (dish?.components ?? []).map((c) => [c.itemId, c.quantity]),
      ),
    )
    createState.reset()
    updateState.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, dish, defaultCategoryId])

  // A combo is made of plain dishes only, never of itself or another combo.
  const choices = categories.flatMap((category) =>
    category.items.filter(
      (item) => item.components.length === 0 && item.id !== dish?.id,
    ),
  )
  const components = choices
    .filter((item) => (quantities[item.id] ?? 0) > 0)
    .map((item) => ({ itemId: item.id, quantity: quantities[item.id] }))

  function changeQuantity(itemId: string, delta: number) {
    setQuantities((current) => ({
      ...current,
      [itemId]: Math.min(
        MAX_QUANTITY,
        Math.max(0, (current[itemId] ?? 0) + delta),
      ),
    }))
  }

  const parsedPrice = parsePrice(price)
  const valid =
    categoryId !== '' &&
    name.trim() !== '' &&
    parsedPrice !== null &&
    (!combo || components.length > 0)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!valid || parsedPrice === null) return
    const body = {
      categoryId,
      name: name.trim(),
      description: description.trim() || null,
      price: parsedPrice,
      components: combo ? components : [],
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
              {combo
                ? dish
                  ? 'Kombonu redaktə et'
                  : 'Yeni kombo'
                : dish
                  ? 'Məhsulu redaktə et'
                  : 'Yeni məhsul'}
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
              placeholder={
                combo ? 'Məsələn, Biznes lanç' : 'Məsələn, Sezar salatı'
              }
              maxLength={150}
            />
          </div>
          {combo && (
            <fieldset className="space-y-2">
              <legend className="text-sm font-medium">Tərkib</legend>
              {choices.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  Əvvəlcə menyuya adi məhsullar əlavə edin.
                </p>
              ) : (
                <ul className="max-h-56 divide-y overflow-y-auto rounded-md border">
                  {choices.map((item) => {
                    const count = quantities[item.id] ?? 0
                    return (
                      <li
                        key={item.id}
                        className="flex items-center justify-between gap-2 px-3 py-2"
                      >
                        <span
                          className={
                            count > 0
                              ? 'text-sm font-medium'
                              : 'text-sm text-muted-foreground'
                          }
                        >
                          {item.name}
                        </span>
                        <div className="flex items-center gap-1">
                          <Button
                            type="button"
                            variant="outline"
                            size="icon"
                            className="size-8"
                            onClick={() => changeQuantity(item.id, -1)}
                            disabled={count === 0}
                            aria-label={`${item.name} azalt`}
                          >
                            <Minus aria-hidden />
                          </Button>
                          <span
                            className="w-6 text-center text-sm tabular-nums"
                            aria-label={`${item.name} sayı`}
                          >
                            {count}
                          </span>
                          <Button
                            type="button"
                            variant="outline"
                            size="icon"
                            className="size-8"
                            onClick={() => changeQuantity(item.id, 1)}
                            disabled={count === MAX_QUANTITY}
                            aria-label={`${item.name} artır`}
                          >
                            <Plus aria-hidden />
                          </Button>
                        </div>
                      </li>
                    )
                  })}
                </ul>
              )}
              <p className="text-xs text-muted-foreground">
                Komboya ən azı bir məhsul seçin. Qiyməti aşağıda özünüz təyin
                edirsiniz.
              </p>
            </fieldset>
          )}
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
