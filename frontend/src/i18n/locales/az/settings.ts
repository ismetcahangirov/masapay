// Azerbaijani texts of the "settings" namespace: the source the other languages follow.
export default {
  page: {
    title: 'Tənzimləmələr',
    description: 'Restoranın xidmət haqqı, POS və ödəniş ayarları',
    noRestaurant: 'Hesabınıza hələ restoran təyin olunmayıb.',
    loading: 'Yüklənir',
    loadFailed: 'Tənzimləmələri yükləmək alınmadı.',
  },
  serviceCharge: {
    title: 'Xidmət haqqı',
    description:
      'Aktivdirsə, hər hesaba məhsulların cəminin bu faizi qədər xidmət haqqı əlavə olunur və müştərinin çekində ayrıca sətir kimi görünür.',
    enabled: 'Xidmət haqqı tətbiq olunsun',
    percent: 'Faiz (%)',
    percentInvalid: '0 ilə 100 arasında bir faiz yazın, məsələn 10',
  },
  pos: {
    title: 'POS sistemi',
    description:
      'Restoranın kassa proqramı. iiko seçilərsə, hesablar ondan oxunur və ödənişdən sonra orada avtomatik bağlanır.',
    iiko: 'iiko',
    micros: 'Micros',
    custom: 'Custom',
    iikoApiLogin: 'iiko API login',
    organizationId: 'Organization ID',
    paymentTypeId: 'masapay ödəniş növünün ID-si',
    uuidInvalid: 'UUID formatında olmalıdır',
  },
  payriff: {
    title: 'Payriff',
    description:
      'Gizli açar heç vaxt tam göstərilmir, yalnız son 4 simvolu görünür.',
    merchantId: 'Merchant ID',
    secretKey: 'Secret Key',
    managedByPlatform:
      'Payriff ödəniş ayarları (Merchant ID, Secret Key) masapay platforması tərəfindən idarə olunur. Dəyişiklik lazımdırsa, bizimlə əlaqə saxlayın.',
  },
  secret: {
    setHint: '{{masked}} (dəyişmək üçün yenisini yazın)',
    notSet: 'Daxil edilməyib',
  },
  form: {
    saving: 'Yadda saxlanılır...',
    save: 'Yadda saxla',
    saved: 'Yadda saxlanıldı',
    saveFailed: 'Yadda saxlamaq alınmadı. Yenidən cəhd edin.',
  },
}
