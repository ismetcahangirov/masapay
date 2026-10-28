import { useRef, useState } from 'react'
import { ArrowRight, Menu } from 'lucide-react'
import {
  motion,
  useMotionValue,
  useReducedMotion,
  useSpring,
} from 'framer-motion'
import { useTranslation } from 'react-i18next'
import { useScrollDirection } from '@/hooks/use-scroll-direction'
import { useUnderHeaderTone } from '@/hooks/use-under-header-tone'
import { BrandMark } from '@/components/layout/brand-mark'
import { LanguageSwitcher } from '@/components/layout/language-switcher'
import { Button } from '@/components/ui/button'
import { LiquidButton } from '@/components/ui/liquid-glass-button'
import {
  Sheet,
  SheetContent,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'
import { cn } from '@/lib/utils'

// Labels are keys of the "landing" texts (nav.*).
const NAV_LINKS = [
  { label: 'product', href: '#product' },
  { label: 'how', href: '#how' },
  { label: 'pricing', href: '#pricing' },
] as const

// Fixed header that hides on scroll-down and reveals on scroll-up (#66). Its
// background is transparent; text/logo contrast the section beneath (dark over
// light sections, light over dark). The CTA is a liquid-glass button that
// switches its glass tint (light/dark) with the same context.
export function SiteHeader() {
  const { direction, atTop } = useScrollDirection()
  const reduceMotion = useReducedMotion()
  const tone = useUnderHeaderTone()
  const { t } = useTranslation('landing')

  const hidden = !reduceMotion && direction === 'down' && !atTop
  // Background is always transparent; the text/logo contrast the section
  // beneath — light text over a dark section, dark text over a light one.
  const onDark = tone === 'dark'

  return (
    <motion.header
      initial={false}
      animate={{ y: hidden ? '-100%' : '0%' }}
      transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
      className={cn(
        'fixed inset-x-0 top-0 z-50 bg-transparent transition-colors duration-300',
        onDark ? 'text-white' : 'text-[#0a0a0a]',
      )}
    >
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-6">
        <a
          href="#top"
          className="flex items-center gap-2 text-xl font-normal tracking-tight lg:text-2xl"
        >
          <BrandMark className="h-8 lg:h-9" />
          masapay
        </a>

        <nav className="hidden items-center gap-8 md:flex">
          {NAV_LINKS.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className={cn(
                'text-base transition-colors',
                onDark
                  ? 'text-white/60 hover:text-white'
                  : 'text-black/55 hover:text-black',
              )}
            >
              {t(`nav.${link.label}`)}
            </a>
          ))}
        </nav>

        {/* The language choice stays visible on phones too, next to the burger. */}
        <div className="flex items-center gap-3">
          <LanguageSwitcher
            className={cn(
              'rounded-full bg-transparent',
              onDark ? 'border-white/30' : 'border-black/20',
            )}
          />
          <div className="hidden md:block">
            <GlassCta onDark={onDark} />
          </div>

          <MobileMenu />
        </div>
      </div>
    </motion.header>
  )
}

function scrollToSection(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}

// Below md the nav and CTA move into a full-height sheet behind a burger
// button. The section scroll waits until the sheet has closed, because the
// open sheet locks page scrolling.
function MobileMenu() {
  const [open, setOpen] = useState(false)
  const pending = useRef<string | null>(null)
  const { t } = useTranslation('landing')

  function go(id: string) {
    pending.current = id
    setOpen(false)
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <button
          type="button"
          aria-label={t('header.openMenu')}
          className="-mr-2 flex size-11 items-center justify-center rounded-full md:hidden"
        >
          <Menu className="size-7" aria-hidden />
        </button>
      </SheetTrigger>
      <SheetContent
        side="right"
        aria-describedby={undefined}
        className="flex w-full flex-col border-l-0 bg-brand-greige px-6 pb-8 pt-0 text-[#0a0a0a] sm:max-w-sm"
        onCloseAutoFocus={(event) => {
          event.preventDefault()
          const id = pending.current
          pending.current = null
          if (id) window.setTimeout(() => scrollToSection(id), 0)
        }}
      >
        <SheetTitle className="sr-only">{t('header.menu')}</SheetTitle>
        <div className="flex h-16 items-center gap-2 text-xl tracking-tight">
          <BrandMark className="h-8" />
          masapay
        </div>
        <nav className="mt-10 flex flex-col gap-6">
          {NAV_LINKS.map((link) => (
            <a
              key={link.href}
              href={link.href}
              onClick={(event) => {
                event.preventDefault()
                go(link.href.slice(1))
              }}
              className="text-3xl tracking-tight"
            >
              {t(`nav.${link.label}`)}
            </a>
          ))}
        </nav>
        <Button
          onClick={() => go('demo')}
          className="mt-auto h-14 w-full rounded-full text-lg"
        >
          {t('requestDemo')}
          <ArrowRight className="size-5" aria-hidden />
        </Button>
      </SheetContent>
    </Sheet>
  )
}

// Liquid-glass CTA with a pointer-reactive brand-green light: while hovering,
// a soft green blob follows the cursor behind the glass, and the glass
// displacement refracts it — the reference video's effect, on hover, using only
// the signature green (no gradients or purple). Clipped to the button footprint
// and disabled under prefers-reduced-motion.
function GlassCta({ onDark }: { onDark: boolean }) {
  const ref = useRef<HTMLDivElement>(null)
  const reduceMotion = useReducedMotion()
  const mx = useMotionValue(0)
  const my = useMotionValue(0)
  const x = useSpring(mx, { stiffness: 260, damping: 26, mass: 0.4 })
  const y = useSpring(my, { stiffness: 260, damping: 26, mass: 0.4 })
  const [active, setActive] = useState(false)
  const { t } = useTranslation('landing')

  function handleMove(event: React.MouseEvent<HTMLDivElement>) {
    const el = ref.current
    if (!el) return
    const rect = el.getBoundingClientRect()
    mx.set(event.clientX - rect.left)
    my.set(event.clientY - rect.top)
  }

  return (
    <div
      ref={ref}
      className="relative"
      onMouseEnter={() => setActive(true)}
      onMouseLeave={() => setActive(false)}
      onMouseMove={handleMove}
    >
      <div className="pointer-events-none absolute inset-0 -z-10 overflow-hidden rounded-full">
        <motion.span
          className="absolute left-0 top-0 -ml-7 -mt-7 size-14 rounded-full bg-brand-green blur-[13px]"
          style={reduceMotion ? undefined : { x, y }}
          animate={{ opacity: active && !reduceMotion ? 0.85 : 0 }}
          transition={{ duration: 0.3 }}
        />
      </div>
      <LiquidButton
        size="lg"
        onClick={() =>
          document
            .getElementById('demo')
            ?.scrollIntoView({ behavior: 'smooth' })
        }
        className={cn(
          'rounded-full text-base font-medium',
          onDark ? 'dark text-white' : 'text-black',
        )}
      >
        {t('requestDemo')}
      </LiquidButton>
    </div>
  )
}
