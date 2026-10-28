import type { Messages } from '@/i18n/messages'
import type az from '../az/tables'

const ru: Messages<typeof az> = {
  page: {
    title: 'Столы',
    subtitle:
      'У каждого стола свой QR-код. Гость сканирует его, видит счёт и оплачивает.',
    preparing: 'Подготовка...',
    downloadAll: 'Скачать все (ZIP)',
    zipFileName: '{{name}}-qr-kody.zip',
    zipFallbackName: 'stoly',
    newTablePlaceholder: 'Название нового стола, например Стол 7',
    newTableLabel: 'Название нового стола',
    add: 'Добавить',
    duplicate: 'Стол с таким названием уже есть.',
    createFailed: 'Не удалось добавить стол.',
    zipFailed: 'Не удалось скачать ZIP.',
    loading: 'Загрузка',
    loadFailed: 'Не удалось загрузить столы.',
    empty: 'Столов пока нет.',
    emptyHint: ' Добавьте первый стол выше.',
  },
  qrCard: {
    qrAlt: 'QR-код: {{label}}',
    open: 'Открыть',
    serviceCharge: 'Сервисный сбор: {{value}}',
    editLabel: 'Сервисный сбор: {{label}}',
    edit: 'Изменить',
  },
  serviceCharge: {
    none: 'Нет',
    restaurant: '{{percent}} (ресторан)',
    thisTable: '{{percent}} (этот стол)',
  },
  serviceChargeDialog: {
    title: 'Сервисный сбор',
    description:
      '{{label}}: какой сервисный сбор добавлять к счетам этого стола. Применяется при добавлении новой позиции.',
    restaurant: 'Как в ресторане',
    own: 'Свой процент',
    none: 'Нет',
    percentLabel: 'Процент (%)',
    percentInvalid:
      'Введите число больше 0 и не больше 100, например 15 или 12,5',
    saveFailed: 'Не удалось сохранить.',
    cancel: 'Отмена',
    saving: 'Сохранение...',
    save: 'Сохранить',
  },
}

export default ru
