import 'i18next'
import type { resources } from './resources'

// Typed keys: t('...') only accepts keys that exist in the Azerbaijani texts.
declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'common'
    resources: (typeof resources)['az']
  }
}
