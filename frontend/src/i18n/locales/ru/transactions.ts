import type { Messages } from '@/i18n/messages'
import type az from '../az/transactions'

const ru: Messages<typeof az> = {
  status: {
    PENDING: 'Ожидает',
    SUCCESS: 'Успешно',
    FAILED: 'Неуспешно',
    REFUNDED: 'Возвращено',
  },
  split: {
    FULL: 'Полностью',
    ITEM: 'По позициям',
    EQUAL: 'Поровну',
  },
  files: {
    excel: 'operacii.xlsx',
  },
  refund: {
    title: 'Вернуть платёж',
    description:
      '{{table}}: {{amount}} будет полностью возвращено на карту гостя. Это действие нельзя отменить.',
    reason: 'Причина (необязательно)',
    reasonPlaceholder: 'Например, оплачена неверная сумма',
    cancel: 'Отмена',
    refunding: 'Возврат...',
    refund: 'Вернуть',
    errors: {
      windowExpired:
        'С момента оплаты прошло больше 30 дней: Payriff не разрешает возврат. Платёж остаётся без изменений.',
      refused: 'Payriff отклонил возврат. Платёж остаётся без изменений.',
      unavailable: 'Payriff сейчас не отвечает. Попробуйте чуть позже.',
      generic: 'Не удалось вернуть платёж. Попробуйте ещё раз.',
    },
  },
  page: {
    title: 'Операции',
    subtitle: 'Все платежи Payriff. Успешный платёж можно вернуть отсюда.',
    preparing: 'Подготовка...',
    downloadExcel: 'Скачать Excel',
    excelFailed: 'Не удалось скачать Excel.',
    noRestaurant: 'К вашему аккаунту пока не привязан ресторан.',
    filters: 'Фильтры',
    searchPlaceholder: 'ID заказа Payriff, ID операции или стол',
    search: 'Поиск',
    from: 'С',
    to: 'По',
    status: 'Статус',
    allStatuses: 'Все статусы',
    splitType: 'Тип оплаты',
    allSplitTypes: 'Все типы',
    clearFilters: 'Сбросить фильтры',
    loadFailed: 'Не удалось загрузить операции.',
    date: 'Дата',
    table: 'Стол',
    amount: 'Сумма',
    tip: 'Чаевые',
    total: 'Итого',
    payriffId: 'Payriff ID',
    action: 'Действие',
    refund: 'Вернуть',
    empty: 'Операции не найдены.',
    loading: 'Загрузка',
    previousPage: 'Предыдущая страница',
    nextPage: 'Следующая страница',
  },
}

export default ru
