import type { Messages } from '@/i18n/messages'
import type az from '../az/admin'

const ru: Messages<typeof az> = {
  layout: {
    toggleMenu: 'Открыть / закрыть меню',
  },
  sidebar: {
    navigation: 'Навигация админ-панели',
    platform: 'Платформа',
    restaurant: 'Ресторан',
    title: 'Боковая панель',
    description: 'Показывает боковую панель на телефоне.',
    toggle: 'Открыть / закрыть боковую панель',
  },
  nav: {
    restaurants: 'Рестораны',
    analytics: 'Аналитика',
    dashboard: 'Дашборд',
    live: 'Столы онлайн',
    tables: 'Столы',
    orders: 'Заказы',
    kitchen: 'Кухня',
    menu: 'Меню',
    transactions: 'Операции',
    staff: 'Сотрудники',
    tips: 'Чаевые',
    myTips: 'Мои чаевые',
    settings: 'Настройки',
  },
  subscription: {
    expiredTitle: 'Подписка закончилась',
    expiredBody:
      '(последний день {{endsOn}}). Гости не могут оплатить онлайн, но заказывать по-прежнему можно. Для продления свяжитесь с masapay.',
    lastDay: 'Сегодня последний день подписки',
    daysLeft_one: 'До конца подписки остался {{count}} день',
    daysLeft_few: 'До конца подписки осталось {{count}} дня',
    daysLeft_many: 'До конца подписки осталось {{count}} дней',
    daysLeft_other: 'До конца подписки осталось {{count}} дня',
    endingBody:
      '(последний день {{endsOn}}). После окончания гости не смогут оплатить онлайн.',
  },
}

export default ru
