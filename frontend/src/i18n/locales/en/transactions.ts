import type { Messages } from '@/i18n/messages'
import type az from '../az/transactions'

const en: Messages<typeof az> = {
  status: {
    PENDING: 'Pending',
    SUCCESS: 'Successful',
    FAILED: 'Failed',
    REFUNDED: 'Refunded',
  },
  split: {
    FULL: 'Full',
    ITEM: 'By item',
    EQUAL: 'Equal',
  },
  files: {
    excel: 'transactions.xlsx',
  },
  refund: {
    title: 'Refund payment',
    description:
      "{{table}}: {{amount}} will be refunded in full to the customer's card. This cannot be undone.",
    reason: 'Reason (optional)',
    reasonPlaceholder: 'For example, the wrong amount was paid',
    cancel: 'Cancel',
    refunding: 'Refunding...',
    refund: 'Refund',
    errors: {
      windowExpired:
        'More than 30 days have passed since the payment: Payriff does not allow a refund. The payment stays as it is.',
      refused: 'Payriff declined the refund. The payment stays as it is.',
      unavailable: 'Payriff is not responding right now. Try again shortly.',
      generic: 'Could not refund. Try again.',
    },
  },
  page: {
    title: 'Transactions',
    subtitle:
      'All Payriff payments. You can refund a successful payment from here.',
    preparing: 'Preparing...',
    downloadExcel: 'Download Excel',
    excelFailed: 'Could not download the Excel file.',
    noRestaurant: 'No restaurant is assigned to your account yet.',
    filters: 'Filters',
    searchPlaceholder: 'Payriff order ID, transaction ID or table',
    search: 'Search',
    from: 'From',
    to: 'To',
    status: 'Status',
    allStatuses: 'All statuses',
    splitType: 'Payment type',
    allSplitTypes: 'All types',
    clearFilters: 'Clear filters',
    loadFailed: 'Could not load the transactions.',
    date: 'Date',
    table: 'Table',
    amount: 'Amount',
    tip: 'Tip',
    total: 'Total',
    payriffId: 'Payriff ID',
    action: 'Action',
    refund: 'Refund',
    empty: 'No transactions found.',
    loading: 'Loading',
    previousPage: 'Previous page',
    nextPage: 'Next page',
  },
}

export default en
