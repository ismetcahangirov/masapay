import { TriangleAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import type { TableTokenInvalidReason } from '@/lib/table-session'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

// Translation keys of the title and text for each reason.
const REASON_COPY = {
  missing: {
    title: 'invalidToken.missingTitle',
    description: 'invalidToken.missingDescription',
  },
  malformed: {
    title: 'invalidToken.malformedTitle',
    description: 'invalidToken.malformedDescription',
  },
} as const satisfies Record<
  TableTokenInvalidReason,
  { title: string; description: string }
>

export function InvalidTokenPage({
  reason,
}: {
  reason: TableTokenInvalidReason
}) {
  const { t } = useTranslation('customer')
  const copy = REASON_COPY[reason]

  return (
    <main className="flex min-h-svh items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
            <TriangleAlert className="size-6" />
          </div>
          <CardTitle>{t(copy.title)}</CardTitle>
          <CardDescription>{t(copy.description)}</CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          {t('invalidToken.help')}
        </CardContent>
      </Card>
    </main>
  )
}
