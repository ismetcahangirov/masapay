import type { Messages } from '@/i18n/messages'
import type az from '../az/auth'

const ru: Messages<typeof az> = {
  login: {
    title: 'masapay admin',
    subtitle: 'Войдите с аккаунтом Google',
    notConfigured:
      'Вход через Google не настроен: укажите VITE_GOOGLE_CLIENT_ID в файле frontend/.env.local.',
    checking: 'Проверяем...',
    pending:
      'Ваш аккаунт создан и ждёт подтверждения менеджера ресторана или администратора. После подтверждения войдите снова.',
    failed: 'Не удалось войти. Попробуйте ещё раз.',
  },
  devLogin: {
    title: 'Локальный тестовый вход (без Google)',
    admin: 'Тестовый админ (SUPER_ADMIN)',
    manager: 'Тестовый менеджер (Dolma Restoran)',
    waiter: 'Тестовый официант (Dolma Restoran)',
    chef: 'Тестовый повар (Dolma Restoran)',
    failed:
      'Тестовый вход не удался. Backend нужно запустить с MASAPAY_DEV_LOGIN_ENABLED=true.',
  },
  logout: 'Выйти',
  guard: {
    checkingSession: 'Проверяем сессию',
    noAccessTitle: 'У вас нет доступа',
    noAccessBody:
      'Этот раздел только для менеджеров ресторанов и администраторов.',
  },
}

export default ru
