import { CircleAlert, ReceiptText, RotateCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

// Loading, error and empty states of the live bill (#20). They share the bill
// card's width so the page does not jump when the data arrives.

export function BillSkeleton() {
  return (
    <Card className="w-full max-w-md" aria-busy="true" aria-live="polite">
      <CardHeader>
        <div className="h-6 w-32 animate-pulse rounded bg-muted" />
        <div className="h-4 w-24 animate-pulse rounded bg-muted" />
      </CardHeader>
      <CardContent className="space-y-3">
        {[0, 1, 2, 3].map((row) => (
          <div key={row} className="flex justify-between gap-3">
            <div className="h-4 w-40 animate-pulse rounded bg-muted" />
            <div className="h-4 w-16 animate-pulse rounded bg-muted" />
          </div>
        ))}
        <span className="sr-only">Hesab yüklənir</span>
      </CardContent>
    </Card>
  )
}

export function BillError({
  notFound,
  onRetry,
}: {
  notFound: boolean
  onRetry: () => void
}) {
  return (
    <Card className="w-full max-w-md" role="alert">
      <CardHeader>
        <div className="flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
          <CircleAlert className="size-6" aria-hidden />
        </div>
        <CardTitle>
          {notFound ? 'Masa tapılmadı' : 'Hesabı yükləmək alınmadı'}
        </CardTitle>
        <CardDescription>
          {notFound
            ? 'Bu QR kod heç bir masaya aid deyil. Zəhmət olmasa masadakı QR kodu yenidən skan edin.'
            : 'İnternet bağlantınızı yoxlayıb yenidən cəhd edin.'}
        </CardDescription>
      </CardHeader>
      {!notFound && (
        <CardContent>
          <Button onClick={onRetry} className="w-full gap-2">
            <RotateCw className="size-4" aria-hidden />
            Yenidən cəhd et
          </Button>
        </CardContent>
      )}
    </Card>
  )
}

export function NoOpenBill({
  tableLabel,
  restaurantName,
}: {
  tableLabel: string
  restaurantName: string
}) {
  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
          <ReceiptText className="size-6" aria-hidden />
        </div>
        <CardTitle>{tableLabel}</CardTitle>
        <CardDescription>{restaurantName}</CardDescription>
      </CardHeader>
      <CardContent className="text-sm text-muted-foreground">
        Bu masada hələ açıq hesab yoxdur. Sifariş verildikdən sonra burada
        görünəcək.
      </CardContent>
    </Card>
  )
}
