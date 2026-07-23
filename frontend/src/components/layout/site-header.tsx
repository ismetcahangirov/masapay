import { useRef, useState } from 'react'
import {
  motion,
  useMotionValue,
  useReducedMotion,
  useSpring,
} from 'framer-motion'
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

        <GlassCta onDark={onDark} />
      </div>
    </motion.header>
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
  )
}
