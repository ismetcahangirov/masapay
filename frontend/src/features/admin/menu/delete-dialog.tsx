import { Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

// "Are you sure?" before a category or dish is deleted (#137).
export function DeleteDialog({
  title,
  description,
  error,
  busy,
  onConfirm,
  onClose,
}: {
  /** null keeps the dialog closed. */
  title: string | null
  description: string
  error: string | null
  busy: boolean
  onConfirm: () => void
  onClose: () => void
}) {
  return (
    <Dialog open={title !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <DialogFooter className="gap-2">
          <Button variant="outline" onClick={onClose}>
            İmtina
          </Button>
          <Button variant="destructive" onClick={onConfirm} disabled={busy}>
            <Trash2 aria-hidden />
            {busy ? 'Silinir...' : 'Sil'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
