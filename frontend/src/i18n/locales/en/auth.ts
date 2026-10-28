import type { Messages } from '@/i18n/messages'
import type az from '../az/auth'

const en: Messages<typeof az> = {
  login: {
    title: 'masapay admin',
    subtitle: 'Sign in with your Google account',
    notConfigured:
      'Google sign-in is not configured: set VITE_GOOGLE_CLIENT_ID in frontend/.env.local.',
    checking: 'Checking...',
    pending:
      'Your account has been created and is waiting for approval by a restaurant manager or an administrator. Sign in again once it is approved.',
    failed: 'Sign-in failed. Please try again.',
  },
  devLogin: {
    title: 'Local test sign-in (without Google)',
    admin: 'Test admin (SUPER_ADMIN)',
    manager: 'Test manager (Dolma Restoran)',
    waiter: 'Test waiter (Dolma Restoran)',
    chef: 'Test chef (Dolma Restoran)',
    failed:
      'Test sign-in failed. The backend must be started with MASAPAY_DEV_LOGIN_ENABLED=true.',
  },
  logout: 'Log out',
  guard: {
    checkingSession: 'Checking your session',
    noAccessTitle: 'You do not have access',
    noAccessBody:
      'This section is only for restaurant managers and administrators.',
  },
}

export default en
