import { useEffect, useRef, useState } from 'react'

export type ScrollDirection = 'up' | 'down'

export interface ScrollState {
  direction: ScrollDirection
  /** True while the page is within `topThreshold` px of the very top. */
  atTop: boolean
}

export interface UseScrollDirectionOptions {
  /** Minimum scroll delta (px) before a direction change is registered. */
  threshold?: number
  /** Distance from the top (px) still considered "at top". */
  topThreshold?: number
}

// Tracks vertical scroll direction with a small threshold so tiny jitters do
// not flip the result. Reads are batched into requestAnimationFrame to stay off
// the scroll hot path. Drives the header hide/reveal behaviour (#66).
export function useScrollDirection({
  threshold = 8,
  topThreshold = 8,
}: UseScrollDirectionOptions = {}): ScrollState {
  const [state, setState] = useState<ScrollState>({
    direction: 'up',
    atTop: true,
  })
  const lastY = useRef(0)
  const ticking = useRef(false)

  useEffect(() => {
    lastY.current = window.scrollY

    function update() {
      ticking.current = false
      const y = Math.max(0, window.scrollY)
      const atTop = y <= topThreshold
      const delta = y - lastY.current

      if (Math.abs(delta) < threshold) {
        // Below the threshold: only the atTop flag may still need updating.
        setState((prev) => (prev.atTop === atTop ? prev : { ...prev, atTop }))
        return
      }

      const direction: ScrollDirection = delta > 0 ? 'down' : 'up'
      lastY.current = y
      setState((prev) =>
        prev.direction === direction && prev.atTop === atTop
          ? prev
          : { direction, atTop },
      )
    }

    function onScroll() {
      if (ticking.current) return
      ticking.current = true
      window.requestAnimationFrame(update)
    }

    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [threshold, topThreshold])

  return state
}
