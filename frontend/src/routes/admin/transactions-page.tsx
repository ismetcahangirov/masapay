import { useEffect, useState } from 'react'
import {
  ChevronLeft,
  ChevronRight,
  FileSpreadsheet,
  LoaderCircle,
  Search,
  Undo2,
  X,
} from 'lucide-react'
import { useMyRestaurantsQuery } from '@/features/admin/settings/settings-api'
import {
  formatAmount,
  formatDateTime,
  SPLIT_LABELS,
  STATUS_LABELS,
} from '@/features/admin/transactions/format'
import { saveFile } from '@/features/admin/tables/tables-api'
import { RefundDialog } from '@/features/admin/transactions/refund-dialog'
import { TransactionStatusBadge } from '@/features/admin/transactions/transaction-status'
import {
  type SplitType,
  type TransactionRow,
  type TransactionStatus,
  useRestaurantTransactionsQuery,
  useTransactionsExcelMutation,
} from '@/features/admin/transactions/transactions-api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { cn } from '@/lib/utils'

const PAGE_SIZE = 20
const SEARCH_DELAY_MS = 300
const ALL = 'ALL'

const STATUSES = Object.keys(STATUS_LABELS) as TransactionStatus[]
const SPLIT_TYPES = Object.keys(SPLIT_LABELS) as SplitType[]

// A filter that narrows the table gets a dark border, so it is obvious at a
// glance which filters are on.
const ACTIVE_FILTER = 'border-foreground font-medium'

// Transactions and refunds (#45): every Payriff payment of a restaurant, with
// search, filters and paging done on the server, and a refund per payment.
export function TransactionsPage() {
  const restaurants = useMyRestaurantsQuery()
  const [chosen, setChosen] = useState<string | null>(null)
  const restaurantId = chosen ?? restaurants.data?.[0]?.id ?? null

  const [searchText, setSearchText] = useState('')
  const [q, setQ] = useState('')
  const [status, setStatus] = useState<TransactionStatus | typeof ALL>(ALL)
  const [splitType, setSplitType] = useState<SplitType | typeof ALL>(ALL)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)
  const [refunding, setRefunding] = useState<TransactionRow | null>(null)

  // Search only after the user stops typing, not on every key.
  useEffect(() => {
    const timer = setTimeout(() => {
      setQ(searchText.trim())
      setPage(0)
    }, SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [searchText])

  const transactions = useRestaurantTransactionsQuery(
    {
      restaurantId: restaurantId ?? '',
      status: status === ALL ? undefined : status,
      splitType: splitType === ALL ? undefined : splitType,
      from: from || undefined,
      to: to || undefined,
      q: q || undefined,
      page,
      size: PAGE_SIZE,
    },
    { skip: !restaurantId },
  )

  const filtered =
    status !== ALL || splitType !== ALL || from !== '' || to !== '' || q !== ''

  function clearFilters() {
    setSearchText('')
    setQ('')
    setStatus(ALL)
    setSplitType(ALL)
    setFrom('')
    setTo('')
    setPage(0)
  }

  // The download follows the same filters as the table (#170).
  const [downloadExcel, excelState] = useTransactionsExcelMutation()
  async function onExcel() {
    if (!restaurantId) return
    try {
      const file = await downloadExcel({
        restaurantId,
        status: status === ALL ? undefined : status,
        splitType: splitType === ALL ? undefined : splitType,
        from: from || undefined,
        to: to || undefined,
        q: q || undefined,
      }).unwrap()
      saveFile(file.blob, file.filename)
    } catch {
      // excelState.isError is shown under the header.
    }
  }

  const data = transactions.data
  const total = data?.totalElements ?? 0
  const first = total === 0 ? 0 : page * PAGE_SIZE + 1
  const last = Math.min((page + 1) * PAGE_SIZE, total)

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-4 sm:p-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">
            Əməliyyatlar
          </h1>
          <p className="text-sm text-muted-foreground">
            Bütün Payriff ödənişləri. Uğurlu ödənişi buradan geri qaytara
            bilərsiniz.
          </p>
        </div>
        <Button
          onClick={() => void onExcel()}
          disabled={!restaurantId || total === 0 || excelState.isLoading}
        >
          <FileSpreadsheet aria-hidden />
          {excelState.isLoading ? 'Hazırlanır...' : 'Excel yüklə'}
        </Button>
      </header>
      {excelState.isError && (
        <p className="text-sm text-destructive">Excel yükləmək alınmadı.</p>
      )}

      {restaurants.data && restaurants.data.length > 1 && (
        <ToggleGroup
          type="single"
          value={restaurantId ?? undefined}
          onValueChange={(value) => {
            if (!value) return
            setChosen(value)
            setPage(0)
          }}
          className="flex-wrap justify-start"
        >
          {restaurants.data.map((restaurant) => (
            <ToggleGroupItem key={restaurant.id} value={restaurant.id}>
              {restaurant.name}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      )}

      {restaurants.data?.length === 0 && (
        <p className="text-sm text-muted-foreground">
          Hesabınıza hələ restoran təyin olunmayıb.
        </p>
      )}

      <section className="space-y-4" aria-label="Filtrlər">
        <div className="flex flex-wrap items-end gap-4">
          <div className="relative min-w-64 flex-1">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
              aria-hidden
            />
            <Input
              value={searchText}
              onChange={(event) => setSearchText(event.target.value)}
              placeholder="Payriff sifariş ID, əməliyyat ID və ya masa"
              aria-label="Axtarış"
              className="pl-9"
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="tx-from">Başlanğıc</Label>
            <Input
              id="tx-from"
              type="date"
              value={from}
              max={to || undefined}
              onChange={(event) => {
                setFrom(event.target.value)
                setPage(0)
              }}
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="tx-to">Son</Label>
            <Input
              id="tx-to"
              type="date"
              value={to}
              min={from || undefined}
              onChange={(event) => {
                setTo(event.target.value)
                setPage(0)
              }}
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="tx-status">Status</Label>
            <Select
              value={status}
              onValueChange={(value) => {
                setStatus(value as TransactionStatus | typeof ALL)
                setPage(0)
              }}
            >
              <SelectTrigger
                id="tx-status"
                className={cn('w-48', status !== ALL && ACTIVE_FILTER)}
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Bütün statuslar</SelectItem>
                {STATUSES.map((value) => (
                  <SelectItem key={value} value={value}>
                    {STATUS_LABELS[value]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1">
            <Label htmlFor="tx-split">Ödəniş tipi</Label>
            <Select
              value={splitType}
              onValueChange={(value) => {
                setSplitType(value as SplitType | typeof ALL)
                setPage(0)
              }}
            >
              <SelectTrigger
                id="tx-split"
                className={cn('w-48', splitType !== ALL && ACTIVE_FILTER)}
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Bütün tiplər</SelectItem>
                {SPLIT_TYPES.map((value) => (
                  <SelectItem key={value} value={value}>
                    {SPLIT_LABELS[value]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          {filtered && (
            <Button variant="ghost" onClick={clearFilters}>
              <X aria-hidden />
              Filtrləri sıfırla
            </Button>
          )}
        </div>
      </section>

      {transactions.isError && (
        <p className="text-sm text-destructive">
          Əməliyyatları yükləmək alınmadı.
        </p>
      )}

      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Tarix</TableHead>
              <TableHead>Masa</TableHead>
              <TableHead>Ödəniş tipi</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Məbləğ</TableHead>
              <TableHead className="text-right">Bəxşiş</TableHead>
              <TableHead className="text-right">Cəmi</TableHead>
              <TableHead>Payriff ID</TableHead>
              <TableHead className="sr-only">Əməliyyat</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data?.items.map((row) => (
              <TableRow key={row.id}>
                <TableCell className="whitespace-nowrap">
                  {formatDateTime(row.createdAt)}
                </TableCell>
                <TableCell>{row.tableLabel}</TableCell>
                <TableCell>
                  {row.splitType ? SPLIT_LABELS[row.splitType] : '-'}
                </TableCell>
                <TableCell>
                  <TransactionStatusBadge status={row.status} />
                </TableCell>
                <TableCell className="whitespace-nowrap text-right tabular-nums">
                  {formatAmount(row.amount, row.currency)}
                </TableCell>
                <TableCell className="whitespace-nowrap text-right tabular-nums">
                  {formatAmount(row.tipAmount, row.currency)}
                </TableCell>
                <TableCell className="whitespace-nowrap text-right font-medium tabular-nums">
                  {formatAmount(row.totalAmount, row.currency)}
                </TableCell>
                <TableCell className="max-w-40 truncate font-mono text-xs text-muted-foreground">
                  {row.payriffOrderId ?? '-'}
                </TableCell>
                <TableCell className="text-right">
                  {row.refundable && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setRefunding(row)}
                    >
                      <Undo2 aria-hidden />
                      Geri qaytar
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {data?.items.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={9}
                  className="py-10 text-center text-muted-foreground"
                >
                  Heç bir əməliyyat tapılmadı.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        {!data && transactions.isFetching && (
          <div className="flex justify-center p-10">
            <LoaderCircle
              className="size-6 animate-spin text-muted-foreground"
              aria-label="Yüklənir"
            />
          </div>
        )}
      </div>

      <div className="flex items-center justify-end gap-4">
        {data && transactions.isFetching && (
          <LoaderCircle
            className="size-4 animate-spin text-muted-foreground"
            aria-label="Yüklənir"
          />
        )}
        <span className="text-sm tabular-nums text-muted-foreground">
          {first}-{last} / {total}
        </span>
        <Button
          variant="outline"
          size="icon"
          onClick={() => setPage((current) => Math.max(0, current - 1))}
          disabled={page === 0}
          aria-label="Əvvəlki səhifə"
        >
          <ChevronLeft aria-hidden />
        </Button>
        <Button
          variant="outline"
          size="icon"
          onClick={() => setPage((current) => current + 1)}
          disabled={last >= total}
          aria-label="Növbəti səhifə"
        >
          <ChevronRight aria-hidden />
        </Button>
      </div>

      {restaurantId && (
        <RefundDialog
          restaurantId={restaurantId}
          transaction={refunding}
          onClose={() => setRefunding(null)}
        />
      )}
    </div>
  )
}
