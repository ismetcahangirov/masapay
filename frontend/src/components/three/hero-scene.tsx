import { lazy, Suspense } from 'react'
import { useReducedMotion } from 'framer-motion'
import { useMediaQuery } from '@/hooks/use-media-query'

// three.js is heavy, so it is code-split behind React.lazy and only loaded on
// large screens. On small screens the component renders nothing (the oversized
// hero heading is the static fallback), so three.js is never downloaded on
// mobile. Reduced-motion users get the object without rotation.
const HeroObject = lazy(() => import('@/components/three/hero-object'))

export function HeroScene({ className }: { className?: string }) {
  const isDesktop = useMediaQuery('(min-width: 1024px)')
  const reduceMotion = useReducedMotion()

  if (!isDesktop) return null

  return (
    <div className={className} aria-hidden>
      <Suspense fallback={null}>
        <HeroObject animate={!reduceMotion} />
      </Suspense>
    </div>
  )
}
