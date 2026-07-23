import type { ReactNode } from 'react'
import { motion, useReducedMotion, type HTMLMotionProps } from 'framer-motion'

export interface RevealProps
  extends Omit<
    HTMLMotionProps<'div'>,
    'initial' | 'whileInView' | 'transition' | 'variants'
  > {
  children: ReactNode
  /** Upward travel distance (px) the content rises from. */
  y?: number
  /** Delay before the reveal starts (s) — useful for staggering. */
  delay?: number
}

// Reveals its children with a fade + upward slide the first time they enter the
// viewport (#67). Animation runs once. When the user prefers reduced motion the
// content is rendered statically at its final state with no transform or fade.
export function Reveal({ children, y = 24, delay = 0, ...props }: RevealProps) {
  const reduceMotion = useReducedMotion()

  return (
    <motion.div
      initial={reduceMotion ? false : { opacity: 0, y }}
      whileInView={reduceMotion ? undefined : { opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.3 }}
      transition={
        reduceMotion
          ? undefined
          : { duration: 0.6, ease: [0.22, 1, 0.36, 1], delay }
      }
      {...props}
    >
      {children}
    </motion.div>
  )
}
