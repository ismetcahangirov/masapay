import { useMemo } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { QrCode } from 'lucide-react'
import { validateTableToken } from '@/lib/table-session'
import { InvalidTokenPage } from '@/routes/invalid-token-page'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

// Landing page reached by scanning a table QR code:
//   /r/:restaurantSlug/t/:tableId?token=<token>
//
// This sub-issue (4.1) establishes the route and the token gate. The live bill,
// data fetching (4.2) and real-time sync (4.3) are layered on top of the
// validated session in later sub-issues.
export function TablePage() {
  const { restaurantSlug = '', tableId = '' } = useParams()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')

  const validation = useMemo(() => validateTableToken(token), [token])

  if (!validation.valid) {
    return <InvalidTokenPage reason={validation.reason} />
  }

  return (
    <main className="flex min-h-svh items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Masa {tableId}</CardTitle>
            <Badge variant="success">
              <QrCode className="mr-1" />
              Aktiv sessiya
            </Badge>
          </div>
          <CardDescription>{restaurantSlug}</CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          Canlı adisyon burada göstəriləcək.
        </CardContent>
      </Card>
    </main>
  )
}
