import type { Messages } from '@/i18n/messages'
import type az from '../az/settings'

const ru: Messages<typeof az> = {
  page: {
    title: 'Настройки',
    description: 'Сервисный сбор, POS и настройки оплаты ресторана',
    noRestaurant: 'К вашему аккаунту пока не привязан ни один ресторан.',
    loading: 'Загрузка',
    loadFailed: 'Не удалось загрузить настройки.',
  },
  serviceCharge: {
    title: 'Сервисный сбор',
    description:
      'Если включено, к каждому счёту добавляется сервисный сбор в этом проценте от суммы блюд, и он виден отдельной строкой в чеке клиента.',
    enabled: 'Применять сервисный сбор',
    percent: 'Процент (%)',
    percentInvalid: 'Укажите процент от 0 до 100, например 10',
  },
  pos: {
    title: 'POS-система',
    description:
      'Кассовая программа ресторана. Если выбран iiko, счета читаются из него и после оплаты автоматически закрываются там.',
    iiko: 'iiko',
    micros: 'Micros',
    custom: 'Custom',
    iikoApiLogin: 'iiko API login',
    organizationId: 'Organization ID',
    paymentTypeId: 'ID типа оплаты masapay',
    uuidInvalid: 'Должен быть в формате UUID',
  },
  payriff: {
    title: 'Payriff',
    description:
      'Секретный ключ никогда не показывается полностью, видны только последние 4 символа.',
    merchantId: 'Merchant ID',
    secretKey: 'Secret Key',
    managedByPlatform:
      'Платёжными настройками Payriff (Merchant ID, Secret Key) управляет платформа masapay. Если нужно что-то изменить, свяжитесь с нами.',
  },
  secret: {
    setHint: '{{masked}} (введите новый, чтобы изменить)',
    notSet: 'Не указано',
  },
  form: {
    saving: 'Сохранение...',
    save: 'Сохранить',
    saved: 'Сохранено',
    saveFailed: 'Не удалось сохранить. Попробуйте ещё раз.',
  },
}

export default ru
