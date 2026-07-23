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
  // Header inverts against the section under it.
  const dark = tone === 'light'

  return (
    <motion.header
      initial={false}
      animate={{ y: hidden ? '-100%' : '0%' }}
      transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
      className={cn(
        'fixed inset-x-0 top-0 z-50 border-b backdrop-blur transition-colors duration-300',
        dark
          ? 'border-white/10 bg-[#0a0a0a]/85 text-white'
          : 'border-black/10 bg-white/85 text-[#0a0a0a]',
      )}
    >
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-6">
        <a
          href="#top"
          className="flex items-center gap-2 text-lg font-medium tracking-tight md:text-xl"
        >
          <img className="w-9 lg:w-11" src="/masapay.svg" alt="masapay" />
          masapay
        </a>

        <nav className="hidden items-center gap-8 md:flex">
          {NAV_LINKS.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className={cn(
                'text-sm transition-colors',
                dark
                  ? 'text-white/60 hover:text-white'
                  : 'text-black/55 hover:text-black',
              )}
            >
              {link.label}
            </a>
          ))}
        </nav>

        <Button variant="success" size="sm" asChild>
          <a href="#demo">Demo istə</a>
        </Button>
      </div>
    </motion.header>
  )
}
