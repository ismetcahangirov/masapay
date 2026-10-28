import { beforeAll, describe, expect, it } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { SiteHeader } from '@/components/layout/site-header'
import i18n from '@/i18n'

beforeAll(() => {
  document.elementFromPoint ??= () => null
})

describe('SiteHeader mobile menu', () => {
  it('opens a sheet with the nav links and the demo CTA', async () => {
    render(<SiteHeader />)

    await userEvent.click(screen.getByRole('button', { name: 'Menyunu aç' }))

    const menu = await screen.findByRole('dialog', { name: 'Menyu' })
    expect(menu).toHaveTextContent('Məhsul')
    expect(menu).toHaveTextContent('Necə işləyir')
    expect(menu).toHaveTextContent('Qiymət')
    expect(
      within(menu).getByRole('button', { name: /Demo istə/ }),
    ).toBeInTheDocument()
  })

  it('closes the sheet when a link is chosen', async () => {
    render(<SiteHeader />)

    await userEvent.click(screen.getByRole('button', { name: 'Menyunu aç' }))
    const menu = await screen.findByRole('dialog', { name: 'Menyu' })
    await userEvent.click(within(menu).getByText('Necə işləyir'))

    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Menyu' })).toBeNull(),
    )
  })
})

describe('SiteHeader in English (#215)', () => {
  it('shows the nav links and the demo CTA in the chosen language', async () => {
    await i18n.changeLanguage('en')
    render(<SiteHeader />)

    // The language choice sits in the header itself, next to the burger.
    expect(
      screen.getByRole('combobox', { name: 'Language' }),
    ).toHaveTextContent('EN')

    await userEvent.click(screen.getByRole('button', { name: 'Open the menu' }))

    const menu = await screen.findByRole('dialog', { name: 'Menu' })
    expect(menu).toHaveTextContent('How it works')
    expect(
      within(menu).getByRole('button', { name: /Request a demo/ }),
    ).toBeInTheDocument()
  })
})
