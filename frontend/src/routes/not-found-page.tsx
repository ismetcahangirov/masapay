import { Compass } from 'lucide-react'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

export function NotFoundPage() {
  return (
    <main className="flex min-h-svh items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
            <Compass className="size-6" />
          </div>
          <CardTitle>Səhifə tapılmadı</CardTitle>
          <CardDescription>
            Axtardığınız səhifə mövcud deyil. Masadakı QR kodu skan edərək
            başlayın.
          </CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          masapay
        </CardContent>
      </Card>
    </main>
  )
}
