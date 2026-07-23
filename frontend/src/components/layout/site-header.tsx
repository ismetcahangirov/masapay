import { motion, useReducedMotion } from 'framer-motion'
import { useScrollDirection } from '@/hooks/use-scroll-direction'
import { useUnderHeaderTone } from '@/hooks/use-under-header-tone'
import { LiquidButton } from '@/components/ui/liquid-glass-button'
import { cn } from '@/lib/utils'

const NAV_LINKS = [
  { label: 'Məhsul', href: '#product' },
  { label: 'Necə işləyir', href: '#how' },
  { label: 'Qiymət', href: '#pricing' },
]

// Fixed header that hides on scroll-down and reveals on scroll-up (#66). Its
// background is transparent; text/logo contrast the section beneath (dark over
// light sections, light over dark). The CTA is a liquid-glass button that
// switches its glass tint (light/dark) with the same context.
export function SiteHeader() {
  const { direction, atTop } = useScrollDirection()
  const reduceMotion = useReducedMotion()
  const tone = useUnderHeaderTone()

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
          className="flex items-center gap-2 text-lg font-normal tracking-tight"
        >
          <img className="w-7 lg:w-8" src="/masapay.svg" alt="masapay" />
          masapay
        </a>

        <nav className="hidden items-center gap-8 md:flex">
          {NAV_LINKS.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className={cn(
                'text-sm transition-colors',
                onDark
                  ? 'text-white/60 hover:text-white'
                  : 'text-black/55 hover:text-black',
              )}
            >
              {link.label}
            </a>
          ))}
        </nav>

        <div className="relative">
          <CtaGlow />
          <LiquidButton
            size="sm"
            onClick={() =>
              document
                .getElementById('demo')
                ?.scrollIntoView({ behavior: 'smooth' })
            }
            className={cn(
              'rounded-full font-medium',
              onDark ? 'dark text-white' : 'text-black',
            )}
          >
            Demo istə
          </LiquidButton>
        </div>
      </div>
    </motion.header>
  )
}

// Subtle brand-green motion parked behind the liquid-glass CTA. It gives the
// glass something to refract (the video effect) using only the signature green
// — no gradients or purple. Clipped to just around the button. Static under
// prefers-reduced-motion.
function CtaGlow() {
  const reduceMotion = useReducedMotion()
  const loop = (duration: number) =>
    reduceMotion
      ? undefined
      : ({
          duration,
          repeat: Infinity,
          repeatType: 'mirror',
          ease: 'easeInOut',
        } as const)

  return (
    <div className="pointer-events-none absolute -inset-3 -z-10 overflow-hidden rounded-full">
      <motion.span
        className="absolute left-1 top-0 size-9 rounded-full bg-brand-green opacity-70 blur-[10px]"
        animate={reduceMotion ? undefined : { x: [0, 55, 12], y: [0, 6, 0] }}
        transition={loop(6)}
      />
      <motion.span
        className="absolute bottom-0 right-2 size-7 rounded-full bg-brand-green opacity-50 blur-[12px]"
        animate={reduceMotion ? undefined : { x: [0, -38, 0], y: [0, -5, 4] }}
        transition={loop(7.5)}
      />
    </div>
  )
}
