import { beforeEach, describe, expect, it } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { store } from '@/app/store'
import { api } from '@/app/api'
import { type UserRole, userLoaded } from '@/features/auth/auth-slice'
import type { IntegrationSettings } from '@/features/admin/settings/settings-api'
import { SettingsForm } from '@/features/admin/settings/settings-form'
import { mockApi, renderWithProviders } from '@/test/utils'

const settings: IntegrationSettings = {
  restaurantId: 'r1',
  posType: 'CUSTOM',
  iikoApiLogin: null,
  iikoApiLoginSet: false,
  iikoOrganizationId: null,
  iikoPaymentTypeId: null,
  payriffMerchantId: 'ES1234567',
  payriffSecretKey: '••••abcd',
  payriffSecretKeySet: true,
  serviceChargePercent: 10,
}

function signIn(role: UserRole) {
  store.dispatch(userLoaded({ id: 'u1', email: 'u@x.az', role }))
}

function backend() {
  let sent: Record<string, unknown> | null = null
  mockApi({
    'PUT /api/admin/restaurants/r1/integration-settings': async (
      request: Request,
    ) => {
      sent = await request.json()
      return settings
    },
  })
  return () => sent
}

describe('SettingsForm Payriff section', () => {
  beforeEach(() => {
    store.dispatch(api.util.resetApiState())
  })

  it('is not shown to a restaurant manager, and a manager never sends it', async () => {
    signIn('RESTO_MANAGER')
    const sent = backend()
    renderWithProviders(
      <SettingsForm settings={{ ...settings, payriffMerchantId: null }} />,
    )

    expect(screen.queryByLabelText('Merchant ID')).toBeNull()
    expect(screen.queryByLabelText('Secret Key')).toBeNull()
    expect(
      screen.getByText(/masapay platforması tərəfindən idarə olunur/),
    ).toBeInTheDocument()

    await userEvent.clear(screen.getByLabelText(/Faiz/))
    await userEvent.type(screen.getByLabelText(/Faiz/), '12')
    await userEvent.click(screen.getByRole('button', { name: 'Yadda saxla' }))

    await waitFor(() => expect(sent()).not.toBeNull())
    expect(sent()).not.toHaveProperty('payriffMerchantId')
    expect(sent()).not.toHaveProperty('payriffSecretKey')
  })

  it('is shown to the super admin and sent', async () => {
    signIn('SUPER_ADMIN')
    const sent = backend()
    renderWithProviders(<SettingsForm settings={settings} />)

    const merchant = screen.getByLabelText('Merchant ID')
    expect(merchant).toHaveValue('ES1234567')
    await userEvent.clear(merchant)
    await userEvent.type(merchant, 'ES7654321')
    await userEvent.click(screen.getByRole('button', { name: 'Yadda saxla' }))

    await waitFor(() =>
      expect(sent()).toMatchObject({ payriffMerchantId: 'ES7654321' }),
    )
  })
})
