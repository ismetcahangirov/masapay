import { motion, useReducedMotion } from 'framer-motion'
import { useScrollDirection } from '@/hooks/use-scroll-direction'
import { Button } from '@/components/ui/button'

const NAV_LINKS = [
  { label: 'Məhsul', href: '#product' },
  { label: 'Necə işləyir', href: '#how' },
  { label: 'Qiymət', href: '#pricing' },
]

// Fixed header that hides on scroll-down and reveals on scroll-up (#66).
// It slides up out of view (translateY -100%) when the guest scrolls down away
// from the top, and slides back down into view on scroll-up or at the top.
// Motion is disabled when the user prefers reduced motion — the header then
// stays visible.
export function SiteHeader() {
  const { direction, atTop } = useScrollDirection()
  const reduceMotion = useReducedMotion()

  const hidden = !reduceMotion && direction === 'down' && !atTop

  return (
    <motion.header
      initial={false}
      animate={{ y: hidden ? '-100%' : '0%' }}
      transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
      className="fixed inset-x-0 top-0 z-50 border-b border-border bg-background/85 backdrop-blur"
    >
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-6">
        <a href="#top" className="text-lg font-medium tracking-tight">
          masapay
        </a>

        <nav className="hidden items-center gap-8 md:flex">
          {NAV_LINKS.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="text-sm text-muted-foreground transition-colors hover:text-foreground"
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
