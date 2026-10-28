import { Languages } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { cn } from '@/lib/utils'
import { LANGUAGES, type Language } from '@/i18n'

// Short codes on the trigger keep it narrow on a phone; the list shows each
// language in its own name, so a visitor finds theirs whatever is shown now.
const CODES: Record<Language, string> = { az: 'AZ', en: 'EN', ru: 'RU' }

/** The AZ / EN / RU choice (#215); the language is remembered per browser. */
export function LanguageSwitcher({ className }: { className?: string }) {
  const { t, i18n } = useTranslation()
  const language = (i18n.resolvedLanguage ?? 'az') as Language

  return (
    <Select
      value={language}
      onValueChange={(value) => void i18n.changeLanguage(value)}
    >
      <SelectTrigger
        aria-label={t('language.label')}
        className={cn('h-9 w-auto gap-1.5 px-3', className)}
      >
        <Languages className="size-4 shrink-0" aria-hidden />
        <SelectValue>{CODES[language]}</SelectValue>
      </SelectTrigger>
      <SelectContent align="end">
        {LANGUAGES.map((code) => (
          <SelectItem key={code} value={code} lang={code}>
            {t(`language.${code}`)}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
