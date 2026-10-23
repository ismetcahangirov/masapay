import { describe, expect, it } from 'vitest'
import { addMonths, bakuToday, daysBetween, formatDay } from './subscription'

describe('subscription days (#148)', () => {
  it('counts today in Baku, which is UTC+4', () => {
    expect(bakuToday(new Date('2026-10-22T21:00:00Z'))).toBe('2026-10-23')
    expect(bakuToday(new Date('2026-10-22T19:00:00Z'))).toBe('2026-10-22')
  })

  it('counts days and adds months without running past a short month', () => {
    expect(daysBetween('2026-10-23', '2026-10-30')).toBe(7)
    expect(daysBetween('2026-10-23', '2026-10-22')).toBe(-1)
    expect(addMonths('2026-10-23', 1)).toBe('2026-11-23')
    expect(addMonths('2026-01-31', 1)).toBe('2026-02-28')
    expect(addMonths('2026-12-15', 1)).toBe('2027-01-15')
  })

  it('shows a day the Azerbaijani way', () => {
    expect(formatDay('2026-10-23')).toBe('23.10.2026')
  })
})
