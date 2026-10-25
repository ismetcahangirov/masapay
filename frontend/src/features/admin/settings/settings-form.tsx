import { useEffect, useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import {
  CircleAlert,
  CircleCheckBig,
  KeyRound,
  Percent,
  Store,
} from 'lucide-react'
import {
  type IntegrationSettings,
  type PosType,
  type UpdateIntegrationSettings,
  useUpdateIntegrationSettingsMutation,
} from '@/features/admin/settings/settings-api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useAppSelector } from '@/app/hooks'
import { cn } from '@/lib/utils'

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

interface FormValues {
  serviceChargeEnabled: boolean
  serviceChargePercent: string
  posType: PosType
  iikoApiLogin: string
  iikoOrganizationId: string
  iikoPaymentTypeId: string
  payriffMerchantId: string
  payriffSecretKey: string
}

// Secrets start empty: the stored value is only hinted (masked) in the
// placeholder, and an empty field means "keep it" (#46: secrets stay masked).
function toFormValues(settings: IntegrationSettings): FormValues {
  return {
    serviceChargeEnabled: settings.serviceChargePercent !== null,
    serviceChargePercent: settings.serviceChargePercent?.toString() ?? '10',
    posType: settings.posType,
    iikoApiLogin: '',
    iikoOrganizationId: settings.iikoOrganizationId ?? '',
    iikoPaymentTypeId: settings.iikoPaymentTypeId ?? '',
    payriffMerchantId: settings.payriffMerchantId ?? '',
    payriffSecretKey: '',
  }
}

/** {@code withPayriff} false (a manager) leaves the Payriff fields out: they are the platform's. */
function toRequest(
  values: FormValues,
  withPayriff: boolean,
): UpdateIntegrationSettings {
  const trimmed = (value: string) => value.trim() || undefined
  return {
    posType: values.posType,
    serviceChargeEnabled: values.serviceChargeEnabled,
    serviceChargePercent: values.serviceChargeEnabled
      ? Number(values.serviceChargePercent.replace(',', '.'))
      : undefined,
    iikoApiLogin: trimmed(values.iikoApiLogin),
    iikoOrganizationId: trimmed(values.iikoOrganizationId),
    iikoPaymentTypeId: trimmed(values.iikoPaymentTypeId),
    ...(withPayriff && {
      // Sent even when blank: a blank merchant id clears it on the server.
      payriffMerchantId: values.payriffMerchantId.trim(),
      payriffSecretKey: trimmed(values.payriffSecretKey),
    }),
  }
}

/** On/off switch built on a button (role="switch"), styled with the design tokens. */
function Switch({
  checked,
  onChange,
  label,
}: {
  checked: boolean
  onChange: (checked: boolean) => void
  label: string
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={() => onChange(!checked)}
      className={cn(
        'relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
        checked ? 'bg-success' : 'bg-muted',
      )}
    >
      <span
        className={cn(
          'inline-block size-5 rounded-full bg-background shadow transition-transform',
          checked ? 'translate-x-5' : 'translate-x-0.5',
        )}
      />
    </button>
  )
}

function FieldError({ message }: { message?: string }) {
  if (!message) return null
  return <p className="text-xs text-destructive">{message}</p>
}

export function SettingsForm({ settings }: { settings: IntegrationSettings }) {
  const [save, saveState] = useUpdateIntegrationSettingsMutation()
  const [saved, setSaved] = useState(false)
  // Payriff (merchant id, secret key) is set by the platform, not by the restaurant.
  const superAdmin =
    useAppSelector((state) => state.auth.user?.role) === 'SUPER_ADMIN'
  const {
    control,
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isDirty },
  } = useForm<FormValues>({ defaultValues: toFormValues(settings) })

  useEffect(() => {
    reset(toFormValues(settings))
  }, [settings, reset])

  const serviceChargeEnabled = watch('serviceChargeEnabled')
  const posType = watch('posType')

  async function onSubmit(values: FormValues) {
    setSaved(false)
    try {
      await save({
        restaurantId: settings.restaurantId,
        body: toRequest(values, superAdmin),
      }).unwrap()
      setSaved(true)
    } catch {
      // saveState.isError shows the message below.
    }
  }

  return (
    <form
      onSubmit={(event) => void handleSubmit(onSubmit)(event)}
      className="space-y-6"
    >
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Percent className="size-5" aria-hidden />
            Xidmət haqqı
          </CardTitle>
          <CardDescription>
            Aktivdirsə, hər hesaba məhsulların cəminin bu faizi qədər xidmət
            haqqı əlavə olunur və müştərinin çekində ayrıca sətir kimi görünür.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between gap-4">
            <Label>Xidmət haqqı tətbiq olunsun</Label>
            <Controller
              control={control}
              name="serviceChargeEnabled"
              render={({ field }) => (
                <Switch
                  checked={field.value}
                  onChange={field.onChange}
                  label="Xidmət haqqı tətbiq olunsun"
                />
              )}
            />
          </div>
          {serviceChargeEnabled && (
            <div className="space-y-1">
              <Label htmlFor="serviceChargePercent">Faiz (%)</Label>
              <Input
                id="serviceChargePercent"
                inputMode="decimal"
                className="max-w-32"
                {...register('serviceChargePercent', {
                  validate: (value) => {
                    const number = Number(value.replace(',', '.'))
                    return (
                      (Number.isFinite(number) &&
                        number > 0 &&
                        number <= 100) ||
                      '0 ilə 100 arasında bir faiz yazın, məsələn 10'
                    )
                  },
                })}
              />
              <FieldError message={errors.serviceChargePercent?.message} />
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Store className="size-5" aria-hidden />
            POS sistemi
          </CardTitle>
          <CardDescription>
            Restoranın kassa proqramı. iiko seçilərsə, hesablar ondan oxunur və
            ödənişdən sonra orada avtomatik bağlanır.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Controller
            control={control}
            name="posType"
            render={({ field }) => (
              <ToggleGroup
                type="single"
                value={field.value}
                onValueChange={(value) => value && field.onChange(value)}
                className="justify-start"
              >
                <ToggleGroupItem value="IIKO">iiko</ToggleGroupItem>
                <ToggleGroupItem value="MICROS">Micros</ToggleGroupItem>
                <ToggleGroupItem value="CUSTOM">Custom</ToggleGroupItem>
              </ToggleGroup>
            )}
          />
          {posType === 'IIKO' && (
            <div className="grid gap-4">
              <div className="space-y-1">
                <Label htmlFor="iikoApiLogin">iiko API login</Label>
                <Input
                  id="iikoApiLogin"
                  type="password"
                  autoComplete="off"
                  placeholder={
                    settings.iikoApiLoginSet
                      ? `${settings.iikoApiLogin} (dəyişmək üçün yenisini yazın)`
                      : 'Daxil edilməyib'
                  }
                  {...register('iikoApiLogin')}
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="iikoOrganizationId">Organization ID</Label>
                <Input
                  id="iikoOrganizationId"
                  placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                  {...register('iikoOrganizationId', {
                    validate: (value) =>
                      !value.trim() ||
                      UUID_PATTERN.test(value.trim()) ||
                      'UUID formatında olmalıdır',
                  })}
                />
                <FieldError message={errors.iikoOrganizationId?.message} />
              </div>
              <div className="space-y-1">
                <Label htmlFor="iikoPaymentTypeId">
                  masapay ödəniş növünün ID-si
                </Label>
                <Input
                  id="iikoPaymentTypeId"
                  placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                  {...register('iikoPaymentTypeId', {
                    validate: (value) =>
                      !value.trim() ||
                      UUID_PATTERN.test(value.trim()) ||
                      'UUID formatında olmalıdır',
                  })}
                />
                <FieldError message={errors.iikoPaymentTypeId?.message} />
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {superAdmin ? (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <KeyRound className="size-5" aria-hidden />
              Payriff
            </CardTitle>
            <CardDescription>
              Gizli açar heç vaxt tam göstərilmir, yalnız son 4 simvolu görünür.
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-4">
            <div className="space-y-1">
              <Label htmlFor="payriffMerchantId">Merchant ID</Label>
              <Input
                id="payriffMerchantId"
                {...register('payriffMerchantId')}
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="payriffSecretKey">Secret Key</Label>
              <Input
                id="payriffSecretKey"
                type="password"
                autoComplete="off"
                placeholder={
                  settings.payriffSecretKeySet
                    ? `${settings.payriffSecretKey} (dəyişmək üçün yenisini yazın)`
                    : 'Daxil edilməyib'
                }
                {...register('payriffSecretKey')}
              />
            </div>
          </CardContent>
        </Card>
      ) : (
        <p className="flex items-start gap-2 rounded-lg border p-4 text-sm text-muted-foreground">
          <KeyRound className="mt-0.5 size-4 shrink-0" aria-hidden />
          Payriff ödəniş ayarları (Merchant ID, Secret Key) masapay platforması
          tərəfindən idarə olunur. Dəyişiklik lazımdırsa, bizimlə əlaqə
          saxlayın.
        </p>
      )}

      <div className="flex flex-wrap items-center gap-4">
        <Button type="submit" disabled={!isDirty || saveState.isLoading}>
          {saveState.isLoading ? 'Yadda saxlanılır...' : 'Yadda saxla'}
        </Button>
        {saved && !isDirty && (
          <span className="flex items-center gap-2 text-sm text-success">
            <CircleCheckBig className="size-4" aria-hidden />
            Yadda saxlanıldı
          </span>
        )}
        {saveState.isError && (
          <span className="flex items-center gap-2 text-sm text-destructive">
            <CircleAlert className="size-4" aria-hidden />
            Yadda saxlamaq alınmadı. Yenidən cəhd edin.
          </span>
        )}
      </div>
    </form>
  )
}
