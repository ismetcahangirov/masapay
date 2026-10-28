import type { Messages } from '@/i18n/messages'
import type az from '../az/admin'

const en: Messages<typeof az> = {
  layout: {
    toggleMenu: 'Open / close the menu',
  },
  sidebar: {
    navigation: 'Admin navigation',
    platform: 'Platform',
    restaurant: 'Restaurant',
    title: 'Sidebar',
    description: 'Displays the mobile sidebar.',
    toggle: 'Toggle sidebar',
  },
  nav: {
    restaurants: 'Restaurants',
    analytics: 'Analytics',
    dashboard: 'Dashboard',
    live: 'Live tables',
    tables: 'Tables',
    orders: 'Orders',
    kitchen: 'Kitchen',
    menu: 'Menu',
    transactions: 'Transactions',
    staff: 'Staff',
    tips: 'Tips',
    myTips: 'My tips',
    settings: 'Settings',
  },
  subscription: {
    expiredTitle: 'Subscription has ended',
    expiredBody:
      '(last day {{endsOn}}). Customers cannot pay online, but ordering is still open. Contact masapay to renew.',
    lastDay: 'Today is the last day of your subscription',
    daysLeft_one: '{{count}} day left on your subscription',
    daysLeft_other: '{{count}} days left on your subscription',
    endingBody:
      '(last day {{endsOn}}). After it ends, customers will not be able to pay online.',
  },
}

export default en
