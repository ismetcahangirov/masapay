import { Download, ExternalLink } from 'lucide-react'
import {
  type AdminTable,
  saveFile,
  useTableQrSvgQuery,
} from '@/features/admin/tables/tables-api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

/** One table: its QR code, a download button and the customer link. */
export function TableQrCard({ table }: { table: AdminTable }) {
  const qr = useTableQrSvgQuery(table.id)

  function download() {
    if (!qr.data) return
    saveFile(
      new Blob([qr.data], { type: 'image/svg+xml' }),
      `${table.label.replace(/[^\p{L}\p{N}_-]+/gu, '-')}.svg`,
    )
  }

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base">{table.label}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex aspect-square items-center justify-center rounded-lg border bg-white p-2">
          {qr.data ? (
            <img
              src={`data:image/svg+xml;charset=utf-8,${encodeURIComponent(qr.data)}`}
              alt={`${table.label} QR kodu`}
              className="size-full"
            />
          ) : (
            <div className="size-full animate-pulse rounded bg-muted" />
          )}
        </div>
        <div className="grid grid-cols-2 gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={download}
            disabled={!qr.data}
          >
            <Download aria-hidden />
            SVG
          </Button>
          <Button variant="outline" size="sm" asChild>
            <a href={table.customerUrl} target="_blank" rel="noreferrer">
              <ExternalLink aria-hidden />
              Aç
            </a>
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
