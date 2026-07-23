import { motion, useReducedMotion } from 'framer-motion'
import { useScrollDirection } from '@/hooks/use-scroll-direction'
import { useUnderHeaderTone } from '@/hooks/use-under-header-tone'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

const NAV_LINKS = [
  { label: 'Məhsul', href: '#product' },
  { label: 'Necə işləyir', href: '#how' },
  { label: 'Qiymət', href: '#pricing' },
]

// Fixed header that hides on scroll-down and reveals on scroll-up (#66) and
// inverts its colours against the section beneath it: over a light section the
// header is dark (near-black bg, light text/logo), over a dark section it is
// light (white bg, dark text/logo). The brand-green CTA reads on both.
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

        <Button
          size="sm"
          asChild
          className={cn(
            onDark
              ? 'bg-white text-black hover:bg-white/90'
              : 'bg-black text-white hover:bg-black/90',
          )}
        >
          <a href="#demo">Demo istə</a>
        </Button>
      </div>
    </motion.header>
  )
}
