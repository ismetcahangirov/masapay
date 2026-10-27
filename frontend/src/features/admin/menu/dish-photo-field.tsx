import { useRef } from 'react'
import { ImagePlus, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { PHOTO_TYPES } from './photo'

/**
 * A dish photo with "choose" and "remove": used when adding or editing a dish
 * one by one and in the review of an imported menu. `src` is what is shown now.
 */
export function DishPhotoField({
  src,
  label,
  onPick,
  onRemove,
  compact = false,
}: {
  src: string | null
  /** The dish name, for screen readers. */
  label: string
  onPick: (file: File) => void
  onRemove: () => void
  /** A small square for table rows. */
  compact?: boolean
}) {
  const input = useRef<HTMLInputElement>(null)
  return (
    <div className={cn('flex items-center gap-2', compact && 'relative')}>
      <button
        type="button"
        onClick={() => input.current?.click()}
        className={cn(
          'flex shrink-0 items-center justify-center overflow-hidden rounded-md border bg-muted text-muted-foreground',
          compact ? 'size-12' : 'size-20',
        )}
        aria-label={src ? `${label} şəklini dəyiş` : `${label} üçün şəkil seç`}
      >
        {src ? (
          <img src={src} alt="" className="size-full object-cover" />
        ) : (
          <ImagePlus className="size-5" aria-hidden />
        )}
      </button>
      {!compact && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => input.current?.click()}
        >
          <ImagePlus aria-hidden />
          {src ? 'Şəkli dəyiş' : 'Şəkil seç'}
        </Button>
      )}
      {src && (
        <Button
          type="button"
          variant="ghost"
          size="icon"
          // In a table row the remove button sits on the photo's corner, so
          // rows with and without a photo stay aligned.
          className={
            compact
              ? 'absolute -right-2 -top-2 size-6 rounded-full border bg-background [&_svg]:size-3'
              : undefined
          }
          onClick={onRemove}
          aria-label={`${label} şəklini sil`}
        >
          <X aria-hidden />
        </Button>
      )}
      <input
        ref={input}
        type="file"
        accept={PHOTO_TYPES}
        className="hidden"
        data-testid="dish-photo-input"
        onChange={(event) => {
          const file = event.target.files?.[0]
          if (file) onPick(file)
          // Choosing the same file again should fire onChange again.
          event.target.value = ''
        }}
      />
    </div>
  )
}
