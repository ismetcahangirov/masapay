import { useTranslation } from 'react-i18next'
import { Download, ExternalLink, Percent } from 'lucide-react'
import {
  type AdminTable,
  saveFile,
  useTableQrSvgQuery,
} from '@/features/admin/tables/tables-api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { serviceChargeLabel } from './service-charge'

/**
 * One table: its QR code, a download button, the customer link and its service charge (#146),
 * which a manager can change from here.
 */
export function TableQrCard({
  table,
  onEditServiceCharge,
}: {
  table: AdminTable
  /** Shown to managers only. */
  onEditServiceCharge?: () => void
}) {
  const { t } = useTranslation('tables')
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
              alt={t('qrCard.qrAlt', { label: table.label })}
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
              {t('qrCard.open')}
            </a>
          </Button>
        </div>
        <div className="flex items-center justify-between gap-2 text-sm">
          <span className="text-muted-foreground">
            {t('qrCard.serviceCharge', { value: serviceChargeLabel(table) })}
          </span>
          {onEditServiceCharge && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onEditServiceCharge}
              aria-label={t('qrCard.editLabel', { label: table.label })}
            >
              <Percent aria-hidden />
              {t('qrCard.edit')}
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
