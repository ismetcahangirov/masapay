import { useState } from 'react'
import { HandCoins, LoaderCircle, ReceiptText } from 'lucide-react'
import { KpiCard } from '@/features/admin/dashboard/kpi-card'
import { bakuToday, formatDay } from '@/features/admin/platform/subscription'
import { useMyTipsQuery } from '@/features/admin/tips/my-tips-api'
import { formatCurrency } from '@/features/order/format'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

const DAY = /^\d{4}-\d{2}-\d{2}$/

const TIME = new Intl.DateTimeFormat('az-AZ', {
  timeZone: 'Asia/Baku',
  day: '2-digit',
  month: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

// "Bəxşişlərim" (#182): the signed-in waiter's own tips, this month by
// default, each with the table it came from. Only their own: the backend takes
// the waiter from the session.
export function MyTipsPage() {
  const today = bakuToday()
  const [from, setFrom] = useState(`${today.slice(0, 8)}01`)
  const [to, setTo] = useState(today)
  const valid = DAY.test(from) && DAY.test(to) && from <= to
  const tips = useMyTipsQuery({ from, to }, { skip: !valid })
  const data = tips.data

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-6">
      <header className="space-y-1">
        <h1 className="flex items-center gap-2 text-2xl font-semibold tracking-tight">
          <HandCoins className="size-6" aria-hidden />
          Bəxşişlərim
        </h1>
        <p className="text-sm text-muted-foreground">
          Xidmət etdiyiniz masalardan gələn bəxşişlər. Götürdüyünüz və ya ilk
          sifarişini siz əlavə etdiyiniz masalar sizindir.
        </p>
      </header>

      <div className="flex flex-wrap items-end gap-4">
        <div className="space-y-1">
          <Label htmlFor="my-tips-from">Başlanğıc</Label>
          <Input
            id="my-tips-from"
            type="date"
            value={from}
            max={to}
            onChange={(event) => setFrom(event.target.value)}
          />
        </div>
        <div className="space-y-1">
          <Label htmlFor="my-tips-to">Son</Label>
          <Input
            id="my-tips-to"
            type="date"
            value={to}
            min={from}
            onChange={(event) => setTo(event.target.value)}
          />
        </div>
      </div>
      {!valid && (
        <p className="text-sm text-destructive">
          Başlanğıc günü son gündən sonra ola bilməz.
        </p>
      )}

      {tips.isFetching && !data && (
        <LoaderCircle
          className="size-6 animate-spin text-muted-foreground"
          aria-label="Yüklənir"
        />
      )}
      {tips.isError && (
        <p className="text-sm text-destructive">
          Bəxşişləri yükləmək alınmadı.
        </p>
      )}

      {data && (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            <KpiCard
              label="Cəmi bəxşiş"
              value={formatCurrency(data.tipTotalMinor, data.currency)}
              hint={`${formatDay(data.from)} - ${formatDay(data.to)}`}
              icon={HandCoins}
            />
            <KpiCard
              label="Bəxşişli ödəniş"
              value={String(data.tipCount)}
              icon={ReceiptText}
            />
          </div>

          <div className="overflow-x-auto rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Vaxt</TableHead>
                  <TableHead>Masa</TableHead>
                  <TableHead className="text-right">Hesab</TableHead>
                  <TableHead className="text-right">Bəxşiş</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.tips.map((tip) => (
                  <TableRow key={tip.id}>
                    <TableCell className="whitespace-nowrap tabular-nums">
                      {TIME.format(new Date(tip.paidAt))}
                    </TableCell>
                    <TableCell className="font-medium">
                      {tip.tableLabel}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatCurrency(tip.billMinor, data.currency)}
                    </TableCell>
                    <TableCell className="text-right font-medium tabular-nums">
                      {formatCurrency(tip.tipMinor, data.currency)}
                    </TableCell>
                  </TableRow>
                ))}
                {data.tips.length === 0 && (
                  <TableRow>
                    <TableCell
                      colSpan={4}
                      className="py-10 text-center text-muted-foreground"
                    >
                      Bu dövrdə bəxşiş yoxdur.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </>
      )}
    </div>
  )
}
