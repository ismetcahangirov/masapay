import { TriangleAlert } from 'lucide-react'
import type { TableTokenInvalidReason } from '@/lib/table-session'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

const REASON_COPY: Record<
  TableTokenInvalidReason,
  { title: string; description: string }
> = {
  missing: {
    title: 'QR kod tanınmadı',
    description:
      'Bu keçid masa məlumatını daşımır. Zəhmət olmasa masadakı QR kodu yenidən skan edin.',
  },
  malformed: {
    title: 'Keçid etibarsızdır',
    description:
      'QR kod düzgün oxunmadı. Zəhmət olmasa masadakı QR kodu yenidən skan edin.',
  },
  expired: {
    title: 'Sessiyanın vaxtı bitib',
    description:
      'Bu QR sessiyasının vaxtı bitmişdir. Yeni sessiya üçün masadakı QR kodu yenidən skan edin.',
  },
}

export function InvalidTokenPage({
  reason,
}: {
  reason: TableTokenInvalidReason
}) {
  const copy = REASON_COPY[reason]

  return (
    <main className="flex min-h-svh items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
            <TriangleAlert className="size-6" />
          </div>
          <CardTitle>{copy.title}</CardTitle>
          <CardDescription>{copy.description}</CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          Kömək lazımdırsa restoran işçisi ilə əlaqə saxlayın.
        </CardContent>
      </Card>
    </main>
  )
}
