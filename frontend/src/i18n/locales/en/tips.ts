import type { Messages } from '@/i18n/messages'
import type az from '../az/tips'

const en: Messages<typeof az> = {
  waiter: {
    unassigned: 'Unassigned',
    unnamed: 'Unnamed waiter',
  },
  chart: {
    tip: 'Tips',
  },
  files: {
    report: 'tips-{{from}}-{{to}}',
  },
  report: {
    title: 'Tips',
    subtitle: 'Tips collected per waiter in the selected date range',
    preparing: 'Preparing...',
    downloadExcel: 'Download Excel',
    downloadCsv: 'Download CSV',
    noRestaurant: 'No restaurant is assigned to your account yet.',
    from: 'From',
    to: 'To',
    presets: 'Quick pick',
    today: 'Today',
    week: 'This week',
    month: 'This month',
    pickBoth: 'Pick both dates for the report.',
    loading: 'Loading',
    restaurantsFailed: 'Could not load the restaurants.',
    badRange: 'The start date cannot be after the end date.',
    loadFailed: 'Could not load the tip report.',
    csvFailed: 'Could not download the CSV.',
    excelFailed: 'Could not download the Excel file.',
    totalTips: 'Total tips',
    tipCount: 'Number of tips',
    tipCountHint: 'Payments with a tip',
    empty: 'No tips in this date range.',
    byWaiter: 'By waiter',
    byWaiterHint: 'Tips collected by each waiter',
    waiter: 'Waiter',
    email: 'Email',
    count: 'Count',
    sum: 'Total',
  },
  mine: {
    title: 'My tips',
    subtitle:
      'Tips from the tables you served. Tables you took, or whose first order you added, are yours.',
    from: 'From',
    to: 'To',
    badRange: 'The start day cannot be after the end day.',
    loading: 'Loading',
    loadFailed: 'Could not load your tips.',
    total: 'Total tips',
    count: 'Payments with a tip',
    time: 'Time',
    table: 'Table',
    bill: 'Bill',
    tip: 'Tip',
    empty: 'No tips in this period.',
  },
}

export default en
