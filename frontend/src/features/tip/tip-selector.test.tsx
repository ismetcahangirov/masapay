import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import i18n from '@/i18n'
import { TipSelector } from '@/features/tip/tip-selector'

describe('TipSelector', () => {
  it('emits the tip for a preset percentage of the bill', async () => {
    const onChange = vi.fn()
    render(<TipSelector baseMinor={4000} currency="AZN" onChange={onChange} />)

    await userEvent.click(screen.getByRole('radio', { name: '10%' }))

    expect(onChange).toHaveBeenLastCalledWith(400)
  })

  it('accepts a custom amount typed in AZN', async () => {
    const onChange = vi.fn()
    render(<TipSelector baseMinor={4000} currency="AZN" onChange={onChange} />)

    await userEvent.click(screen.getByRole('radio', { name: 'Başqa məbləğ' }))
    const input = screen.getByLabelText('Bəxşiş məbləği (AZN)')
    await userEvent.type(input, '2,5')
    expect(onChange).toHaveBeenLastCalledWith(250)
  })

  it('speaks English when the guest switches the language (#215)', async () => {
    await i18n.changeLanguage('en')
    render(<TipSelector baseMinor={4000} currency="AZN" />)

    await userEvent.click(screen.getByRole('radio', { name: 'Other amount' }))
    expect(screen.getByLabelText('Tip amount (AZN)')).toBeInTheDocument()
  })
})
