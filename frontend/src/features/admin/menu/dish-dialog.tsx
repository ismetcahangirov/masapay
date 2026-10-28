import { type FormEvent, useEffect, useState } from 'react'
import { Minus, Plus } from 'lucide-react'
import { useTranslation } from 'react-i18next'
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
import { DishPhotoField } from './dish-photo-field'
import {
  type MenuCategory,
  type MenuDish,
  useCreateDishMutation,
  useDeleteDishImageMutation,
  useSetDishImageMutation,
  useUpdateDishMutation,
} from './menu-api'
import { parsePrice } from './menu-price'

const MAX_QUANTITY = 99

// Adds a dish to a category or edits one, including moving it to another
// category (#137). In combo mode it also picks the dishes a combo or business
// lunch is made of, with a quantity each; the combo has its own price (#138).
// The photo customers see is chosen here too and sent after the dish is saved.
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
  const { t } = useTranslation('menu')
  const [categoryId, setCategoryId] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [ingredients, setIngredients] = useState('')
  const [price, setPrice] = useState('')
  const [quantities, setQuantities] = useState<Record<string, number>>({})
  const [photo, setPhoto] = useState<{ file: File; preview: string } | null>(
    null,
  )
  const [removePhoto, setRemovePhoto] = useState(false)
  // A new dish saved before its photo failed: a retry edits it, not adds another.
  const [savedId, setSavedId] = useState<string | null>(null)
  const [create, createState] = useCreateDishMutation()
  const [update, updateState] = useUpdateDishMutation()
  const [setImage, setImageState] = useSetDishImageMutation()
  const [deleteImage, deleteImageState] = useDeleteDishImageMutation()
  const state = dish || savedId ? updateState : createState
  const photoFailed = setImageState.isError || deleteImageState.isError
  const busy =
    state.isLoading || setImageState.isLoading || deleteImageState.isLoading

  useEffect(() => {
    if (!open) return
    setCategoryId(dish?.categoryId ?? defaultCategoryId ?? '')
    setName(dish?.name ?? '')
    setDescription(dish?.description ?? '')
    setIngredients(dish?.ingredients ?? '')
    setPrice(dish ? dish.price.toFixed(2) : '')
    setQuantities(
      Object.fromEntries(
        (dish?.components ?? []).map((c) => [c.itemId, c.quantity]),
      ),
    )
    setPhoto(null)
    setRemovePhoto(false)
    setSavedId(null)
    createState.reset()
    updateState.reset()
    setImageState.reset()
    deleteImageState.reset()
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
      ingredients: ingredients.trim() || null,
      price: parsedPrice,
      components: combo ? components : [],
    }
    try {
      const editing = dish?.id ?? savedId
      const saved = editing
        ? await update({ restaurantId, dishId: editing, body }).unwrap()
        : await create({ restaurantId, body }).unwrap()
      setSavedId(saved.id)
      if (photo) {
        await setImage({
          restaurantId,
          dishId: saved.id,
          file: photo.file,
        }).unwrap()
      } else if (removePhoto && saved.imageUrl) {
        await deleteImage({ restaurantId, dishId: saved.id }).unwrap()
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
                  ? t('dishDialog.editCombo')
                  : t('dishDialog.newCombo')
                : dish
                  ? t('dishDialog.editDish')
                  : t('dishDialog.newDish')}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="dish-category">{t('dishDialog.category')}</Label>
            <Select value={categoryId} onValueChange={setCategoryId}>
              <SelectTrigger id="dish-category">
                <SelectValue
                  placeholder={t('dishDialog.categoryPlaceholder')}
                />
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
            <span className="text-sm font-medium">{t('dishDialog.photo')}</span>
            <DishPhotoField
              src={
                photo?.preview ??
                (removePhoto ? null : (dish?.imageUrl ?? null))
              }
              label={name.trim() || t('dishDialog.photoFallbackLabel')}
              onPick={(file) => {
                setPhoto({ file, preview: URL.createObjectURL(file) })
                setRemovePhoto(false)
              }}
              onRemove={() => {
                setPhoto(null)
                setRemovePhoto(true)
              }}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="dish-name">{t('dishDialog.name')}</Label>
            <Input
              id="dish-name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder={
                combo
                  ? t('dishDialog.comboPlaceholder')
                  : t('dishDialog.dishPlaceholder')
              }
              maxLength={150}
            />
          </div>
          {combo && (
            <fieldset className="space-y-2">
              <legend className="text-sm font-medium">
                {t('dishDialog.contents')}
              </legend>
              {choices.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  {t('dishDialog.noChoices')}
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
                            aria-label={t('dishDialog.decrease', {
                              name: item.name,
                            })}
                          >
                            <Minus aria-hidden />
                          </Button>
                          <span
                            className="w-6 text-center text-sm tabular-nums"
                            aria-label={t('dishDialog.count', {
                              name: item.name,
                            })}
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
                            aria-label={t('dishDialog.increase', {
                              name: item.name,
                            })}
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
                {t('dishDialog.comboHint')}
              </p>
            </fieldset>
          )}
          <div className="space-y-2">
            <Label htmlFor="dish-ingredients">
              {t('dishDialog.ingredients')}
            </Label>
            <Input
              id="dish-ingredients"
              value={ingredients}
              onChange={(event) => setIngredients(event.target.value)}
              placeholder={t('dishDialog.ingredientsPlaceholder')}
              maxLength={300}
              aria-describedby="dish-ingredients-hint"
            />
            <p
              id="dish-ingredients-hint"
              className="text-xs text-muted-foreground"
            >
              {t('dishDialog.ingredientsHint')}
            </p>
          </div>
          <div className="space-y-2">
            <Label htmlFor="dish-description">
              {t('dishDialog.description')}
            </Label>
            <Textarea
              id="dish-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder={t('dishDialog.descriptionPlaceholder')}
              maxLength={500}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="dish-price">{t('dishDialog.price')}</Label>
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
                {t('dishDialog.priceInvalid')}
              </p>
            )}
          </div>
          {state.isError && (
            <p className="text-sm text-destructive">
              {t('actions.saveFailed')}
            </p>
          )}
          {photoFailed && (
            <p className="text-sm text-destructive">
              {t('dishDialog.photoFailed')}
            </p>
          )}
          <DialogFooter className="gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              {t('actions.cancel')}
            </Button>
            <Button type="submit" disabled={!valid || busy}>
              {busy ? t('actions.saving') : t('actions.save')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
