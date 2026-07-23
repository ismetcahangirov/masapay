import { useEffect, useRef, useState } from 'react'

export type SectionTone = 'light' | 'dark'

// Reports the tone of the section currently sitting under the fixed header, by
// sampling the element just below the header band. Sections opt in by setting a
// `data-section-tone="light|dark"` attribute. The header inverts against this
// (light section -> dark header, and vice versa). Sampling is rAF-throttled and
// runs on scroll and resize.
export function useUnderHeaderTone(headerHeight = 64): SectionTone {
  const [tone, setTone] = useState<SectionTone>('light')
  const ticking = useRef(false)

  useEffect(() => {
    function sample() {
      ticking.current = false
      const x = Math.round(window.innerWidth / 2)
      const y = headerHeight + 2
      const el = document.elementFromPoint(x, y)
      const toned = el?.closest<HTMLElement>('[data-section-tone]')
      const next = toned?.dataset.sectionTone
      if (next === 'light' || next === 'dark') {
        setTone((prev) => (prev === next ? prev : next))
      }
    }

    function onScrollOrResize() {
      if (ticking.current) return
      ticking.current = true
      window.requestAnimationFrame(sample)
    }

    sample()
    window.addEventListener('scroll', onScrollOrResize, { passive: true })
    window.addEventListener('resize', onScrollOrResize)
    return () => {
      window.removeEventListener('scroll', onScrollOrResize)
      window.removeEventListener('resize', onScrollOrResize)
    }
  }, [headerHeight])

  return tone
}
