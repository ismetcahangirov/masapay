import { describe, expect, it } from 'vitest'
import { parseTipInput, tipFromPercent } from '@/features/tip/tip'

describe('tipFromPercent', () => {
  it('takes a percentage of the bill in qəpik, rounded', () => {
    expect(tipFromPercent(2550, 10)).toBe(255)
    expect(tipFromPercent(1999, 15)).toBe(300)
    expect(tipFromPercent(5000, 0)).toBe(0)
  })
})

describe('parseTipInput', () => {
  it('reads AZN with a dot or a comma into qəpik', () => {
    expect(parseTipInput('5')).toBe(500)
    expect(parseTipInput('2,5')).toBe(250)
    expect(parseTipInput(' 3.75 ')).toBe(375)
  })

  it('rejects empty, negative, non-numeric and over-precise input', () => {
    expect(parseTipInput('')).toBeNull()
    expect(parseTipInput('-2')).toBeNull()
    expect(parseTipInput('abc')).toBeNull()
    expect(parseTipInput('1.234')).toBeNull()
  })
})
