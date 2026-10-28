import i18n from '@/i18n'
import type { OrderItem } from '@/features/order/types'

/** "Aysel", "Müştəri (QR)" or "POS": who put a line on the bill (#144). */
export function lineAuthor(item: OrderItem): string {
  if (item.source === 'CUSTOMER') return i18n.t('orders:lineAuthor.customer')
  if (item.source === 'POS') return 'POS'
  return item.addedBy ?? i18n.t('orders:lineAuthor.staff')
}
