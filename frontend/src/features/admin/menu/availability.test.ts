import { describe, expect, it } from 'vitest'
import { availabilityLabel, currentMode } from './availability'
import type { MenuDish } from './menu-api'

const dish = (overrides: Partial<MenuDish>): MenuDish => ({
  id: 'd1',
  categoryId: 'c1',
  name: 'Dovğa',
  description: null,
  price: 3,
  components: [],
  active: true,
  offOn: null,
  availableFrom: null,
  availableUntil: null,
  availableToday: true,
  ...overrides,
})

describe('daily menu status (#139)', () => {
  it('describes each mode and starts the dialog in it', () => {
    expect(availabilityLabel(dish({}))).toBeNull()
    expect(currentMode(dish({}))).toBe('ALWAYS')

    const off = dish({ active: false, availableToday: false })
    expect(availabilityLabel(off)).toBe('Söndürülüb')
    expect(currentMode(off)).toBe('OFF')

    const offToday = dish({ offOn: '2026-10-22', availableToday: false })
    expect(availabilityLabel(offToday)).toBe('Bu gün yoxdur')
    expect(currentMode(offToday)).toBe('OFF_TODAY')

    const fromOnly = dish({
      availableFrom: '2026-10-24',
      availableToday: false,
    })
    expect(availabilityLabel(fromOnly)).toBe('24.10 - ...')
    expect(currentMode(fromOnly)).toBe('DATES')
  })

  it('treats yesterday’s "off today" as back on the menu', () => {
    const stale = dish({ offOn: '2026-10-21', availableToday: true })
    expect(availabilityLabel(stale)).toBeNull()
    expect(currentMode(stale)).toBe('ALWAYS')
  })

  it('explains a combo that is off because a dish inside is off', () => {
    const combo = dish({
      components: [{ itemId: 'd2', name: 'Mərci', quantity: 1 }],
      availableToday: false,
    })
    expect(availabilityLabel(combo)).toBe('Tərkibdə bu gün olmayan var')
  })
})
