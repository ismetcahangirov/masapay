import { describe, expect, it } from 'vitest'
import type { CustomerDish } from './customer-menu-api'
import { upsellFor } from './upsell'

const dish = (id: string, suggestionIds: string[] = []): CustomerDish => ({
  id,
  name: id,
  description: null,
  price: 100,
  components: [],
  suggestionIds,
})

describe('upsellFor (#141)', () => {
  const kebab = dish('kebab', ['sauce', 'ayran'])
  const dolma = dish('dolma', ['ayran', 'tea'])
  const sauce = dish('sauce')
  const ayran = dish('ayran')
  const tea = dish('tea')

  it('offers each extra once, in basket and then manager order', () => {
    const extras = upsellFor(
      [
        { dish: kebab, quantity: 1 },
        { dish: dolma, quantity: 1 },
      ],
      [kebab, dolma, sauce, ayran, tea],
    )
    expect(extras.map((d) => d.id)).toEqual(['sauce', 'ayran', 'tea'])
  })

  it('skips extras already in the basket and ones off today’s menu', () => {
    const extras = upsellFor(
      [
        { dish: kebab, quantity: 1 },
        { dish: ayran, quantity: 2 },
      ],
      // "sauce" is not on today's menu.
      [kebab, ayran],
    )
    expect(extras).toEqual([])
  })

  it('copes with a basket saved before suggestions existed', () => {
    const old = { ...kebab } as Partial<CustomerDish>
    delete old.suggestionIds
    expect(
      upsellFor([{ dish: old as CustomerDish, quantity: 1 }], [sauce]),
    ).toEqual([])
  })
})
