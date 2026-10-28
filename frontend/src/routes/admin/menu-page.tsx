import { useState } from 'react'
import {
  CalendarClock,
  FileUp,
  Layers,
  LoaderCircle,
  MessageCircleQuestion,
  Pencil,
  Plus,
  Sparkles,
  Trash2,
} from 'lucide-react'
import { useAppSelector } from '@/app/hooks'
import { availabilityLabel } from '@/features/admin/menu/availability'
import { AvailabilityDialog } from '@/features/admin/menu/availability-dialog'
import { CategoryDialog } from '@/features/admin/menu/category-dialog'
import { DeleteDialog } from '@/features/admin/menu/delete-dialog'
import { DishDialog } from '@/features/admin/menu/dish-dialog'
import { ImportMenuDialog } from '@/features/admin/menu/import-dialog'
import { SuggestionsDialog } from '@/features/admin/menu/suggestions-dialog'
import {
  type MenuCategory,
  type MenuDish,
  useDeleteCategoryMutation,
  useDeleteDishMutation,
  useRestaurantMenuQuery,
  useSetExtraMutation,
} from '@/features/admin/menu/menu-api'
import { isConflict } from '@/features/admin/menu/menu-errors'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import { formatAmount } from '@/features/admin/transactions/format'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { cn } from '@/lib/utils'

type Removing =
  | { kind: 'category'; category: MenuCategory }
  | { kind: 'dish'; dish: MenuDish }

// The restaurant menu (#137). Managers add, edit and delete categories and
// dishes; waiters see the same menu read-only. The customer menu is built from
// this data later (#140). Dishes are added one by one, or a whole menu at once
// from a PDF or Word file, reviewed before it is saved.
export function MenuPage() {
  const role = useAppSelector((state) => state.auth.user?.role)
  const canEdit = role === 'SUPER_ADMIN' || role === 'RESTO_MANAGER'

  const restaurants = useMyRestaurantsQuery()
  const [chosen, setChosen] = useState<string | null>(null)
  const restaurantId = chosen ?? restaurants.data?.[0]?.id ?? null

  const menu = useRestaurantMenuQuery(restaurantId ?? '', {
    skip: !restaurantId,
  })
  const categories = menu.data ?? []

  const [categoryDialog, setCategoryDialog] = useState<{
    category: MenuCategory | null
  } | null>(null)
  const [dishDialog, setDishDialog] = useState<{
    dish: MenuDish | null
    categoryId: string | null
    combo: boolean
  } | null>(null)
  const [removing, setRemoving] = useState<Removing | null>(null)
  const [scheduling, setScheduling] = useState<MenuDish | null>(null)
  const [suggesting, setSuggesting] = useState<MenuDish | null>(null)
  const [importing, setImporting] = useState(false)
  const [setExtra] = useSetExtraMutation()
  const dishNames = new Map(
    categories.flatMap((c) => c.items).map((item) => [item.id, item.name]),
  )

  const [deleteCategory, deleteCategoryState] = useDeleteCategoryMutation()
  const [deleteDish, deleteDishState] = useDeleteDishMutation()
  const deleteState =
    removing?.kind === 'category' ? deleteCategoryState : deleteDishState

  const nextSortOrder =
    categories.reduce((max, c) => Math.max(max, c.sortOrder), -1) + 1

  function closeDelete() {
    deleteCategoryState.reset()
    deleteDishState.reset()
    setRemoving(null)
  }

  async function onConfirmDelete() {
    if (!restaurantId || !removing) return
    try {
      if (removing.kind === 'category') {
        await deleteCategory({
          restaurantId,
          categoryId: removing.category.id,
        }).unwrap()
      } else {
        await deleteDish({ restaurantId, dishId: removing.dish.id }).unwrap()
      }
      closeDelete()
    } catch {
      // deleteState.error is shown in the dialog.
    }
  }

  const deleteError = !deleteState.isError
    ? null
    : !isConflict(deleteState.error)
      ? 'Silmək alınmadı.'
      : removing?.kind === 'category'
        ? 'Bu kateqoriyada məhsul var. Əvvəlcə məhsulları silin və ya başqa kateqoriyaya köçürün.'
        : 'Bu məhsul kombonun tərkibindədir. Əvvəlcə onu kombodan çıxarın.'

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-4 sm:p-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">Menyu</h1>
          <p className="text-sm text-muted-foreground">
            Kateqoriyalar və məhsullar. Müştəri menyusu bu siyahıdan gəlir.
          </p>
        </div>
        {canEdit && restaurantId && (
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={() => setImporting(true)}>
              <FileUp aria-hidden />
              Menyunu fayldan yüklə
            </Button>
            <Button onClick={() => setCategoryDialog({ category: null })}>
              <Plus aria-hidden />
              Kateqoriya əlavə et
            </Button>
          </div>
        )}
      </header>

      {restaurants.data && restaurants.data.length > 1 && (
        <ToggleGroup
          type="single"
          value={restaurantId ?? undefined}
          onValueChange={(value) => value && setChosen(value)}
          className="flex-wrap justify-start"
        >
          {restaurants.data.map((restaurant) => (
            <ToggleGroupItem key={restaurant.id} value={restaurant.id}>
              {restaurant.name}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      )}

      {menu.isLoading && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label="Yüklənir"
        />
      )}
      {menu.isError && (
        <p className="text-sm text-destructive">Menyunu yükləmək alınmadı.</p>
      )}
      {menu.data?.length === 0 && (
        <p className="text-sm text-muted-foreground">
          Menyu hələ boşdur.
          {canEdit &&
            ' İlk kateqoriyanı əlavə edin, sonra ona məhsullar, və ya bütün menyunu PDF/Word faylından yükləyin.'}
        </p>
      )}

      <div className="space-y-4">
        {categories.map((category) => (
          <Card
            key={category.id}
            role="region"
            aria-labelledby={`category-${category.id}`}
          >
            <CardHeader className="flex-row flex-wrap items-center justify-between gap-2 space-y-0">
              <CardTitle className="text-lg">
                <h2 id={`category-${category.id}`}>{category.name}</h2>
              </CardTitle>
              {canEdit && (
                <div className="flex gap-1">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      setDishDialog({
                        dish: null,
                        categoryId: category.id,
                        combo: false,
                      })
                    }
                  >
                    <Plus aria-hidden />
                    Məhsul
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      setDishDialog({
                        dish: null,
                        categoryId: category.id,
                        combo: true,
                      })
                    }
                  >
                    <Layers aria-hidden />
                    Kombo
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => setCategoryDialog({ category })}
                    aria-label={`${category.name} kateqoriyasını redaktə et`}
                  >
                    <Pencil aria-hidden />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => setRemoving({ kind: 'category', category })}
                    aria-label={`${category.name} kateqoriyasını sil`}
                  >
                    <Trash2 aria-hidden />
                  </Button>
                </div>
              )}
            </CardHeader>
            <CardContent>
              {category.items.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  Bu kateqoriyada məhsul yoxdur.
                </p>
              ) : (
                <ul className="divide-y">
                  {category.items.map((dish) => {
                    const status = availabilityLabel(dish)
                    return (
                      <li
                        key={dish.id}
                        className="flex flex-col gap-2 py-3 sm:flex-row sm:items-start sm:justify-between sm:gap-4"
                      >
                        {dish.imageUrl && (
                          <img
                            src={dish.imageUrl}
                            alt=""
                            loading="lazy"
                            className={cn(
                              'size-16 shrink-0 rounded-md object-cover',
                              !dish.availableToday && 'opacity-60',
                            )}
                          />
                        )}
                        <div
                          className={cn(
                            'min-w-0 flex-1 space-y-0.5',
                            !dish.availableToday && 'opacity-60',
                          )}
                        >
                          <p className="flex flex-wrap items-center gap-2 font-medium">
                            {dish.name}
                            {dish.components.length > 0 && (
                              <Badge variant="secondary">Kombo</Badge>
                            )}
                            {dish.askBeforeConfirm && (
                              <Badge variant="outline">
                                <MessageCircleQuestion
                                  className="mr-1 size-3"
                                  aria-hidden
                                />
                                Təsdiqdən əvvəl soruşulur
                              </Badge>
                            )}
                            {status && (
                              <Badge variant="outline">
                                <CalendarClock
                                  className="mr-1 size-3"
                                  aria-hidden
                                />
                                {status}
                              </Badge>
                            )}
                          </p>
                          {dish.components.length > 0 && (
                            <p className="text-sm">
                              {dish.components
                                .map((c) => `${c.quantity} x ${c.name}`)
                                .join(', ')}
                            </p>
                          )}
                          {dish.ingredients && (
                            <p className="text-sm">
                              <span className="text-muted-foreground">
                                Tərkib:{' '}
                              </span>
                              {dish.ingredients}
                            </p>
                          )}
                          {dish.description && (
                            <p className="text-sm text-muted-foreground">
                              {dish.description}
                            </p>
                          )}
                          {dish.suggestionIds.length > 0 && (
                            <p className="flex items-center gap-1 text-xs text-muted-foreground">
                              <Sparkles className="size-3" aria-hidden />
                              Təklif:{' '}
                              {dish.suggestionIds
                                .map((id) => dishNames.get(id))
                                .filter(Boolean)
                                .join(', ')}
                            </p>
                          )}
                        </div>
                        <div className="flex flex-wrap items-center gap-1 sm:shrink-0 sm:flex-nowrap">
                          <span className="mr-2 w-full font-medium tabular-nums sm:w-auto">
                            {formatAmount(dish.price, 'AZN')}
                          </span>
                          {canEdit && (
                            <>
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => setScheduling(dish)}
                                aria-label={`${dish.name} mövcudluğu`}
                              >
                                <CalendarClock aria-hidden />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => setSuggesting(dish)}
                                aria-label={`${dish.name} əlavə təklifləri`}
                              >
                                <Sparkles aria-hidden />
                              </Button>
                              <Button
                                variant={
                                  dish.askBeforeConfirm ? 'secondary' : 'ghost'
                                }
                                size="icon"
                                aria-pressed={dish.askBeforeConfirm}
                                onClick={() =>
                                  restaurantId &&
                                  void setExtra({
                                    restaurantId,
                                    dishId: dish.id,
                                    askBeforeConfirm: !dish.askBeforeConfirm,
                                  })
                                }
                                aria-label={`${dish.name} təsdiqdən əvvəl soruşulsun`}
                                title="Təsdiqdən əvvəl hər müştəridən soruşulsun (sous, su)"
                              >
                                <MessageCircleQuestion aria-hidden />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() =>
                                  setDishDialog({
                                    dish,
                                    categoryId: null,
                                    combo: dish.components.length > 0,
                                  })
                                }
                                aria-label={`${dish.name} redaktə et`}
                              >
                                <Pencil aria-hidden />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() =>
                                  setRemoving({ kind: 'dish', dish })
                                }
                                aria-label={`${dish.name} sil`}
                              >
                                <Trash2 aria-hidden />
                              </Button>
                            </>
                          )}
                        </div>
                      </li>
                    )
                  })}
                </ul>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      {restaurantId && (
        <>
          <CategoryDialog
            restaurantId={restaurantId}
            open={categoryDialog !== null}
            category={categoryDialog?.category ?? null}
            nextSortOrder={nextSortOrder}
            onClose={() => setCategoryDialog(null)}
          />
          <DishDialog
            restaurantId={restaurantId}
            open={dishDialog !== null}
            dish={dishDialog?.dish ?? null}
            combo={dishDialog?.combo ?? false}
            categories={categories}
            defaultCategoryId={dishDialog?.categoryId ?? null}
            onClose={() => setDishDialog(null)}
          />
          <AvailabilityDialog
            restaurantId={restaurantId}
            dish={scheduling}
            onClose={() => setScheduling(null)}
          />
          <ImportMenuDialog
            restaurantId={restaurantId}
            open={importing}
            menu={categories}
            onClose={() => setImporting(false)}
          />
          <SuggestionsDialog
            restaurantId={restaurantId}
            dish={suggesting}
            categories={categories}
            onClose={() => setSuggesting(null)}
          />
        </>
      )}
      <DeleteDialog
        title={
          removing === null
            ? null
            : removing.kind === 'category'
              ? 'Kateqoriyanı sil'
              : 'Məhsulu sil'
        }
        description={
          removing === null
            ? ''
            : removing.kind === 'category'
              ? `"${removing.category.name}" kateqoriyası silinəcək. Əminsiniz?`
              : `"${removing.dish.name}" menyudan silinəcək. Əminsiniz?`
        }
        error={deleteError}
        busy={deleteState.isLoading}
        onConfirm={() => void onConfirmDelete()}
        onClose={closeDelete}
      />
    </div>
  )
}
