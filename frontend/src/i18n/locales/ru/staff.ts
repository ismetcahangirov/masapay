import type { Messages } from '@/i18n/messages'
import type az from '../az/staff'

const ru: Messages<typeof az> = {
  roles: {
    WAITER: 'Официант',
    CHEF: 'Повар',
  },
  status: {
    INVITED: 'Приглашён, ещё не входил',
    PENDING: 'Ждёт подтверждения',
    ACTIVE: 'Активен',
    DEACTIVATED: 'Отключён',
  },
  page: {
    title: 'Сотрудники',
    description:
      'Пригласите официантов и поваров по почте их Google-аккаунта. Подтвердите их здесь после того, как они войдут через Google.',
    inviteForm: 'Пригласить сотрудника',
    email: 'Google-почта',
    emailPlaceholder: 'imya.familiya@gmail.com',
    role: 'Роль',
    invite: 'Пригласить',
    emailTaken: 'Эта почта уже принадлежит сотруднику или другому ресторану.',
    inviteFailed: 'Не удалось пригласить. Проверьте почту.',
    updateFailed: 'Не удалось сохранить изменение.',
    loading: 'Загрузка',
    loadFailed: 'Не удалось загрузить сотрудников.',
    columns: {
      member: 'Сотрудник',
      role: 'Роль',
      status: 'Статус',
      actions: 'Действия',
    },
    roleAria: 'Роль {{email}}',
    approveAria: 'Подтвердить {{email}}',
    approve: 'Подтвердить',
    activate: 'Включить',
    deactivateAria: 'Отключить {{email}}',
    deactivate: 'Отключить',
    empty:
      'Сотрудников пока нет. Пригласите первого официанта или повара выше.',
  },
}

export default ru
