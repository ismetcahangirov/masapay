import { Compass } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

export function NotFoundPage() {
  const { t } = useTranslation('customer')
  return (
    <main className="flex min-h-svh items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
            <Compass className="size-6" />
          </div>
          <CardTitle>{t('notFound.title')}</CardTitle>
          <CardDescription>{t('notFound.description')}</CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          masapay
        </CardContent>
      </Card>
    </main>
  )
}
