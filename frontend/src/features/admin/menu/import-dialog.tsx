import { useEffect, useRef, useState } from 'react'
import {
  CircleCheck,
  FileUp,
  LoaderCircle,
  Plus,
  Trash2,
  TriangleAlert,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
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
import { problemReason } from '@/lib/api-errors'
import { cn } from '@/lib/utils'
import { DishPhotoField } from './dish-photo-field'
import {
  type ImportCategory,
  type ImportDraft,
  type ImportResult,
  type MenuCategory,
  useApplyImportMutation,
  usePreviewImportMutation,
} from './menu-api'
import { parsePrice } from './menu-price'
import { photoDataUrl } from './photo'

/** The menu file types we read: PDF with text, Word .docx and old .doc. */
const MENU_FILES =
  '.pdf,.docx,.doc,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/msword'

interface Row {
  key: string
  name: string
  description: string
  price: string
  image: string | null
}

interface Group {
  key: string
  name: string
  rows: Row[]
}

let nextKey = 0
const newKey = () => `k${++nextKey}`

const nameKey = (name: string) => name.trim().toLocaleLowerCase('az')

function toGroups(draft: ImportDraft): Group[] {
  return draft.categories.map((category) => ({
    key: newKey(),
    name: category.name,
    rows: category.items.map((item) => ({
      key: newKey(),
      name: item.name,
      description: item.description ?? '',
      price: item.price.toFixed(2),
      image: item.image,
    })),
  }))
}

function previewError(error: Parameters<typeof problemReason>[0]): string {
  if (error && 'status' in error && error.status === 413) {
    return 'Fayl 10 MB-dan böyükdür.'
  }
  if (error && 'status' in error && error.status === 415) {
    return 'Bu fayl növü oxunmur. PDF, DOCX və ya DOC faylı seçin.'
  }
  switch (problemReason(error)) {
    case 'NO_TEXT':
      return 'PDF-də mətn tapılmadı: bu, skan edilmiş şəkil ola bilər. Mətnli PDF və ya Word faylı seçin.'
    case 'NO_DISHES':
      return 'Faylda qiyməti olan məhsul tapılmadı. Başqa fayl seçin və ya məhsulları tək-tək əlavə edin.'
    case 'PROTECTED_FILE':
      return 'Fayl parolla qorunur. Parolsuz nüsxəni yükləyin.'
    case 'TOO_MANY_PAGES':
      return 'Faylda 60-dan çox səhifə var.'
    default:
      return 'Faylı oxumaq alınmadı.'
  }
}

// The whole menu at once from a PDF or Word file. The file is read and turned
// into categories and dishes by the backend, but nothing is saved yet: the
// manager checks every line, fixes names and prices, adds or removes dishes
// and photos, and only "Təsdiqlə" saves it. A dish or category whose name is
// already on the menu is updated instead of added twice. Adding dishes one by
// one keeps working as before.
export function ImportMenuDialog({
  restaurantId,
  open,
  menu,
  onClose,
}: {
  restaurantId: string
  open: boolean
  /** The current menu, to show what will be updated rather than added. */
  menu: MenuCategory[]
  onClose: () => void
}) {
  const fileInput = useRef<HTMLInputElement>(null)
  const [groups, setGroups] = useState<Group[] | null>(null)
  const [skippedImages, setSkippedImages] = useState(0)
  const [result, setResult] = useState<ImportResult | null>(null)
  const [preview, previewState] = usePreviewImportMutation()
  const [apply, applyState] = useApplyImportMutation()

  useEffect(() => {
    if (!open) return
    setGroups(null)
    setResult(null)
    setSkippedImages(0)
    previewState.reset()
    applyState.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  const menuCategories = new Set(menu.map((c) => nameKey(c.name)))
  const menuDishes = new Set(
    menu.flatMap((c) => c.items).map((item) => nameKey(item.name)),
  )

  async function onFile(file: File) {
    try {
      const draft = await preview({ restaurantId, file }).unwrap()
      setGroups(toGroups(draft))
      setSkippedImages(draft.skippedImages)
    } catch {
      // previewState.error is shown below.
    }
  }

  function changeGroup(key: string, change: (group: Group) => Group) {
    setGroups(
      (current) =>
        current && current.map((g) => (g.key === key ? change(g) : g)),
    )
  }

  function changeRow(groupKey: string, rowKey: string, patch: Partial<Row>) {
    changeGroup(groupKey, (group) => ({
      ...group,
      rows: group.rows.map((r) => (r.key === rowKey ? { ...r, ...patch } : r)),
    }))
  }

  async function pickPhoto(groupKey: string, rowKey: string, file: File) {
    try {
      changeRow(groupKey, rowKey, { image: await photoDataUrl(file) })
    } catch {
      // An unreadable file simply leaves the photo as it was.
    }
  }

  const rows = groups?.flatMap((g) => g.rows) ?? []
  const counts = new Map<string, number>()
  rows.forEach((r) =>
    counts.set(nameKey(r.name), (counts.get(nameKey(r.name)) ?? 0) + 1),
  )
  const rowProblem = (row: Row): string | null => {
    if (row.name.trim() === '') return 'Ad yazın'
    if (parsePrice(row.price) === null) return 'Qiyməti rəqəmlə yazın'
    if ((counts.get(nameKey(row.name)) ?? 0) > 1) return 'Bu ad iki dəfədir'
    return null
  }
  const filledGroups = groups?.filter((g) => g.rows.length > 0) ?? []
  const valid =
    rows.length > 0 &&
    filledGroups.every((g) => g.name.trim() !== '') &&
    rows.every((r) => rowProblem(r) === null)

  async function onApply() {
    if (!valid) return
    const categories: ImportCategory[] = filledGroups.map((group) => ({
      name: group.name.trim(),
      existing: menuCategories.has(nameKey(group.name)),
      items: group.rows.map((row) => ({
        name: row.name.trim(),
        description: row.description.trim() || null,
        price: parsePrice(row.price) ?? 0,
        image: row.image,
        existingId: null,
      })),
    }))
    try {
      setResult(await apply({ restaurantId, categories }).unwrap())
    } catch {
      // applyState.error is shown below.
    }
  }

  const applyError = !applyState.isError
    ? null
    : problemReason(applyState.error) === 'DUPLICATE_DISH'
      ? 'Eyni adlı iki məhsul var. Adlardan birini dəyişin.'
      : problemReason(applyState.error) === 'INVALID_IMAGE'
        ? 'Şəkillərdən biri oxunmadı. Onu silin və ya başqa şəkil seçin.'
        : 'Yadda saxlamaq alınmadı.'

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent
        className={cn(
          groups && !result ? 'flex h-[92svh] max-w-5xl flex-col' : 'max-w-md',
        )}
      >
        <DialogHeader>
          <DialogTitle>Menyunu fayldan yüklə</DialogTitle>
          <DialogDescription>
            {result
              ? 'Menyu yeniləndi.'
              : groups
                ? 'Tapılanları yoxlayın: adları, qiymətləri və şəkilləri düzəldin, lazım olmayanı silin, sonra təsdiqləyin. Təsdiqə qədər heç nə saxlanmır.'
                : 'PDF və ya Word (DOCX, DOC) faylındakı menyunu oxuyuruq. Sonra hər sətri yoxlayıb düzəldə bilərsiniz.'}
          </DialogDescription>
        </DialogHeader>

        {result && (
          <div className="space-y-4">
            <p className="flex items-start gap-2 text-sm">
              <CircleCheck className="mt-0.5 size-4 shrink-0" aria-hidden />
              <span>
                {result.dishesCreated} məhsul əlavə olundu,{' '}
                {result.dishesUpdated} məhsul yeniləndi,{' '}
                {result.categoriesCreated} yeni kateqoriya.
              </span>
            </p>
            <DialogFooter>
              <Button onClick={onClose}>Bağla</Button>
            </DialogFooter>
          </div>
        )}

        {!groups && !result && (
          <div className="space-y-4">
            <Button
              className="w-full"
              disabled={previewState.isLoading}
              onClick={() => fileInput.current?.click()}
            >
              {previewState.isLoading ? (
                <LoaderCircle className="animate-spin" aria-hidden />
              ) : (
                <FileUp aria-hidden />
              )}
              {previewState.isLoading ? 'Fayl oxunur...' : 'Fayl seçin'}
            </Button>
            <input
              ref={fileInput}
              type="file"
              accept={MENU_FILES}
              className="hidden"
              data-testid="menu-file-input"
              onChange={(event) => {
                const file = event.target.files?.[0]
                event.target.value = ''
                if (file) void onFile(file)
              }}
            />
            <p className="text-xs text-muted-foreground">
              Ən yaxşı nəticə: hər məhsul bir sətirdə, adı və qiyməti ilə
              (məsələn "Sezar salatı ... 9 AZN"), kateqoriya adları ayrıca
              sətirdə. Skan edilmiş PDF oxunmur. Ən çox 10 MB.
            </p>
            {previewState.isError && (
              <p className="text-sm text-destructive">
                {previewError(previewState.error)}
              </p>
            )}
          </div>
        )}

        {groups && !result && (
          <>
            <div className="min-h-0 flex-1 space-y-4 overflow-y-auto pr-1">
              {skippedImages > 0 && (
                <p className="flex items-start gap-2 text-sm text-muted-foreground">
                  <TriangleAlert
                    className="mt-0.5 size-4 shrink-0"
                    aria-hidden
                  />
                  Fayldakı {skippedImages} şəkil oxunmadı (JPEG, PNG və ya WebP
                  deyil). Onları aşağıda özünüz əlavə edə bilərsiniz.
                </p>
              )}
              {groups.map((group) => (
                <section
                  key={group.key}
                  aria-label={`Kateqoriya ${group.name}`}
                  className="space-y-2 rounded-lg border p-3"
                >
                  <div className="flex flex-wrap items-center gap-2">
                    <Input
                      value={group.name}
                      onChange={(event) =>
                        changeGroup(group.key, (g) => ({
                          ...g,
                          name: event.target.value,
                        }))
                      }
                      maxLength={100}
                      aria-label="Kateqoriya adı"
                      aria-invalid={group.name.trim() === ''}
                      className="max-w-xs font-medium"
                    />
                    {menuCategories.has(nameKey(group.name)) && (
                      <Badge variant="outline">Mövcud kateqoriya</Badge>
                    )}
                    <Button
                      variant="ghost"
                      size="sm"
                      className="ml-auto"
                      onClick={() =>
                        setGroups(
                          (current) =>
                            current &&
                            current.filter((g) => g.key !== group.key),
                        )
                      }
                    >
                      <Trash2 aria-hidden />
                      Kateqoriyanı çıxar
                    </Button>
                  </div>
                  <ul className="divide-y">
                    {group.rows.map((row) => {
                      const problem = rowProblem(row)
                      return (
                        <li
                          key={row.key}
                          className="grid grid-cols-[auto_1fr_auto] items-start gap-2 py-2 md:grid-cols-[auto_minmax(0,14rem)_1fr_7rem_auto]"
                        >
                          <DishPhotoField
                            compact
                            src={row.image}
                            label={row.name || 'Məhsul'}
                            onPick={(file) =>
                              void pickPhoto(group.key, row.key, file)
                            }
                            onRemove={() =>
                              changeRow(group.key, row.key, { image: null })
                            }
                          />
                          <div className="space-y-1">
                            <Input
                              value={row.name}
                              onChange={(event) =>
                                changeRow(group.key, row.key, {
                                  name: event.target.value,
                                })
                              }
                              maxLength={150}
                              aria-label="Məhsulun adı"
                              aria-invalid={problem !== null}
                            />
                            {menuDishes.has(nameKey(row.name)) && (
                              <Badge variant="secondary">
                                Menyuda var, yenilənəcək
                              </Badge>
                            )}
                            {problem && (
                              <p className="text-xs text-destructive">
                                {problem}
                              </p>
                            )}
                          </div>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="md:order-last"
                            onClick={() =>
                              changeGroup(group.key, (g) => ({
                                ...g,
                                rows: g.rows.filter((r) => r.key !== row.key),
                              }))
                            }
                            aria-label={`${row.name || 'Məhsul'} çıxar`}
                          >
                            <Trash2 aria-hidden />
                          </Button>
                          <Input
                            value={row.description}
                            onChange={(event) =>
                              changeRow(group.key, row.key, {
                                description: event.target.value,
                              })
                            }
                            maxLength={500}
                            placeholder="Təsvir"
                            aria-label="Təsvir"
                            className="col-span-2 col-start-2 md:col-span-1 md:col-start-auto"
                          />
                          <Input
                            value={row.price}
                            onChange={(event) =>
                              changeRow(group.key, row.key, {
                                price: event.target.value,
                              })
                            }
                            inputMode="decimal"
                            aria-label="Qiymət (AZN)"
                            aria-invalid={parsePrice(row.price) === null}
                            className="col-span-2 col-start-2 tabular-nums md:col-span-1 md:col-start-auto"
                          />
                        </li>
                      )
                    })}
                  </ul>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      changeGroup(group.key, (g) => ({
                        ...g,
                        rows: [
                          ...g.rows,
                          {
                            key: newKey(),
                            name: '',
                            description: '',
                            price: '',
                            image: null,
                          },
                        ],
                      }))
                    }
                  >
                    <Plus aria-hidden />
                    Məhsul əlavə et
                  </Button>
                </section>
              ))}
              <Button
                variant="outline"
                onClick={() =>
                  setGroups((current) => [
                    ...(current ?? []),
                    { key: newKey(), name: 'Yeni kateqoriya', rows: [] },
                  ])
                }
              >
                <Plus aria-hidden />
                Kateqoriya əlavə et
              </Button>
            </div>
            {applyError && (
              <p className="text-sm text-destructive">{applyError}</p>
            )}
            <DialogFooter className="gap-2">
              <Button variant="outline" onClick={() => setGroups(null)}>
                Başqa fayl
              </Button>
              <Button
                variant="success"
                disabled={!valid || applyState.isLoading}
                onClick={() => void onApply()}
              >
                {applyState.isLoading
                  ? 'Yadda saxlanılır...'
                  : `Təsdiqlə (${rows.length} məhsul)`}
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}
