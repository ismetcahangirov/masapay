import { describe, expect, it } from 'vitest'
import { parsePercent, serviceChargeLabel } from './service-charge'
import type { AdminTable } from './tables-api'

const table = (own: number | null, effective: number | null): AdminTable => ({
  id: 't1',
  label: 'VIP',
  status: 'AVAILABLE',
  qrToken: 'q1',
  customerUrl: 'http://x',
  serviceChargePercent: own,
  effectiveServiceChargePercent: effective,
})

describe('table service charge (#146)', () => {
  it('reads a percentage with a dot or a comma and rejects the rest', () => {
    expect(parsePercent('15')).toBe(15)
    expect(parsePercent(' 12,5 ')).toBe(12.5)
    expect(parsePercent('100')).toBe(100)
    expect(parsePercent('0')).toBeNull()
    expect(parsePercent('101')).toBeNull()
    expect(parsePercent('7.555')).toBeNull()
    expect(parsePercent('on beş')).toBeNull()
  })

  it('says where the table’s charge comes from', () => {
    expect(serviceChargeLabel(table(null, 10))).toBe('10% (restoran)')
    expect(serviceChargeLabel(table(15, 15))).toBe('15% (bu masa)')
    expect(serviceChargeLabel(table(0, null))).toBe('Yoxdur')
    expect(serviceChargeLabel(table(null, null))).toBe('Yoxdur')
  })
})
