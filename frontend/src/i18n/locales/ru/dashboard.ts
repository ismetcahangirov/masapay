import type { Messages } from '@/i18n/messages'
import type az from '../az/dashboard'

const ru: Messages<typeof az> = {
  title: 'Дашборд',
  subtitle: 'Показатели за сегодня',
  noRestaurant: 'К вашему аккаунту пока не привязан ресторан.',
  loading: 'Загрузка',
  loadFailed: 'Не удалось загрузить показатели.',
  kpi: {
    dailyRevenue: 'Выручка за день',
    tipsExcluded: 'Без учёта чаевых',
    tipsCollected: 'Собранные чаевые',
    averageTableTime: 'Среднее время за столом',
    noPaidBills: 'Сегодня оплаченных счетов нет',
    payriffSuccessRate: 'Доля успешных оплат Payriff',
    noPayriffPayments: 'Сегодня оплат через Payriff нет',
  },
  duration: {
    hoursMinutes: '{{hours}} ч {{minutes}} мин',
    minutes: '{{minutes}} мин',
  },
  chart: {
    title: 'Выручка (по дням)',
    description_one: 'Последний {{count}} день, выручка по дням',
    description_few: 'Последние {{count}} дня, выручка по дням',
    description_many: 'Последние {{count}} дней, выручка по дням',
    description_other: 'Последние {{count}} дня, выручка по дням',
    revenue: 'Выручка',
  },
}

export default ru
