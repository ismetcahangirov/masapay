import type { Messages } from '@/i18n/messages'
import type az from '../az/tips'

const ru: Messages<typeof az> = {
  waiter: {
    unassigned: 'Не назначен',
    unnamed: 'Официант без имени',
  },
  chart: {
    tip: 'Чаевые',
  },
  files: {
    report: 'chaevye-{{from}}-{{to}}',
  },
  report: {
    title: 'Чаевые',
    subtitle: 'Чаевые по официантам за выбранный период',
    preparing: 'Подготовка...',
    downloadExcel: 'Скачать Excel',
    downloadCsv: 'Скачать CSV',
    noRestaurant: 'К вашему аккаунту пока не привязан ресторан.',
    from: 'С',
    to: 'По',
    presets: 'Быстрый выбор',
    today: 'Сегодня',
    week: 'Эта неделя',
    month: 'Этот месяц',
    pickBoth: 'Выберите обе даты для отчёта.',
    loading: 'Загрузка',
    restaurantsFailed: 'Не удалось загрузить рестораны.',
    badRange: 'Дата начала не может быть позже даты окончания.',
    loadFailed: 'Не удалось загрузить отчёт по чаевым.',
    csvFailed: 'Не удалось скачать CSV.',
    excelFailed: 'Не удалось скачать Excel.',
    totalTips: 'Всего чаевых',
    tipCount: 'Количество чаевых',
    tipCountHint: 'Оплаты с чаевыми',
    empty: 'За этот период чаевых нет.',
    byWaiter: 'По официантам',
    byWaiterHint: 'Чаевые, собранные каждым официантом',
    waiter: 'Официант',
    email: 'Эл. почта',
    count: 'Кол-во',
    sum: 'Сумма',
  },
  mine: {
    title: 'Мои чаевые',
    subtitle:
      'Чаевые со столов, которые вы обслуживали. Ваши столы: те, что вы взяли, или где вы добавили первый заказ.',
    from: 'С',
    to: 'По',
    badRange: 'День начала не может быть позже дня окончания.',
    loading: 'Загрузка',
    loadFailed: 'Не удалось загрузить чаевые.',
    total: 'Всего чаевых',
    count: 'Оплаты с чаевыми',
    time: 'Время',
    table: 'Стол',
    bill: 'Счёт',
    tip: 'Чаевые',
    empty: 'За этот период чаевых нет.',
  },
}

export default ru
