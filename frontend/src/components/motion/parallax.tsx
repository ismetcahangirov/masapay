import { useRef, type ReactNode } from 'react'
import {
  motion,
  useReducedMotion,
  useScroll,
  useTransform,
} from 'framer-motion'
import { cn } from '@/lib/utils'

export interface ParallaxProps {
  children: ReactNode
  /**
   * Parallax strength. The content is offset by ±(speed * 100)px across the
   * time it travels through the viewport. Positive drifts the content up as the
   * page scrolls down; negative drifts it down.
   */
  speed?: number
  className?: string
}

// Scroll-linked parallax (#68): translates its children vertically as the
// element passes through the viewport, creating depth. Disabled (static) when
// the user prefers reduced motion.
export function Parallax({ children, speed = 0.2, className }: ParallaxProps) {
  const ref = useRef<HTMLDivElement>(null)
  const reduceMotion = useReducedMotion()

  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ['start end', 'end start'],
  })

  const range = 100 * speed
  const y = useTransform(scrollYProgress, [0, 1], [range, -range])

  return (
    <div ref={ref} className={cn('relative', className)}>
      <motion.div style={{ y: reduceMotion ? 0 : y }}>{children}</motion.div>
    </div>
  )
}
