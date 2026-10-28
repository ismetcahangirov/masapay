import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { resources } from './resources'

// The app's languages (#215). Azerbaijani is the default and the source text;
// English and Russian are for tourists and for Russian-speaking staff.
export const LANGUAGES = ['az', 'en', 'ru'] as const
export type Language = (typeof LANGUAGES)[number]

const STORAGE_KEY = 'masapay.language'

// Locales for dates, times and amounts in each language.
const LOCALES: Record<Language, string> = {
  az: 'az-AZ',
  en: 'en-GB',
  ru: 'ru-RU',
}

function isLanguage(value: unknown): value is Language {
  return LANGUAGES.includes(value as Language)
}

function storedLanguage(): Language | null {
  try {
    const value = window.localStorage.getItem(STORAGE_KEY)
    return isLanguage(value) ? value : null
  } catch {
    return null
  }
}

// The language a visitor gets first: the one they chose before, else their
// browser's if we have it (a tourist with an English phone), else Azerbaijani.
function initialLanguage(): Language {
  const stored = storedLanguage()
  if (stored) return stored
  const browser = (navigator.languages ?? [navigator.language])
    .map((tag) => tag.slice(0, 2).toLowerCase())
    .find(isLanguage)
  return browser ?? 'az'
}

void i18n.use(initReactI18next).init({
  resources,
  lng: initialLanguage(),
  fallbackLng: 'az',
  supportedLngs: LANGUAGES,
  defaultNS: 'common',
  interpolation: { escapeValue: false },
})

document.documentElement.lang = i18n.language

i18n.on('languageChanged', (language) => {
  document.documentElement.lang = language
  try {
    window.localStorage.setItem(STORAGE_KEY, language)
  } catch {
    // Private mode or blocked storage: the choice lasts until the page closes.
  }
})

/** The current language (Azerbaijani if i18next reports anything else). */
export function currentLanguage(): Language {
  return isLanguage(i18n.language) ? i18n.language : 'az'
}

/** The Intl locale of the current language, for dates, times and amounts. */
export function currentLocale(): string {
  return LOCALES[currentLanguage()]
}

export default i18n
