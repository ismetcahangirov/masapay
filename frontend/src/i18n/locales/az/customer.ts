// Azerbaijani texts of the "customer" namespace: the source the other languages follow.
export default {
  orderStatus: {
    open: 'Açıq',
    awaiting_payment: 'Ödəniş gözlənilir',
    partially_paid: 'Qismən ödənilib',
    paid: 'Ödənilib',
    cancelled: 'Ləğv olunub',
  },
  kitchenStatus: {
    NEW: 'Qəbul olundu',
    PREPARING: 'Hazırlanır',
    READY: 'Hazırdır',
  },
  orderItemList: {
    paid: 'Ödənilib',
    empty: 'Hələ sifariş əlavə olunmayıb.',
  },
  billCard: {
    equalSplitPaid: 'Bərabər bölgü: {{paid}} / {{total}} hissə ödənilib',
    byItems: 'Məhsul seçərək (xətt çəkilənlər)',
    equalSplitParts: 'Bərabər bölgü ({{paid}} / {{total}} hissə)',
    otherPayments: 'Digər ödənişlər',
    paidProgress: 'Ödənilmiş hissə',
    fullyPaid: 'Hesab tam ödənilib. Təşəkkür edirik!',
    cancelled: 'Sifariş restoran tərəfindən ləğv olundu. Ödəniş tələb olunmur.',
    items: 'Məhsullar',
    serviceCharge: 'Xidmət haqqı',
    serviceChargePercent: 'Xidmət haqqı ({{percent}}%)',
    total: 'Cəmi',
    paid: 'Ödənilib',
    balance: 'Qalıq',
  },
  billStates: {
    loading: 'Hesab yüklənir',
    notFoundTitle: 'Masa tapılmadı',
    errorTitle: 'Hesabı yükləmək alınmadı',
    notFoundDescription:
      'Bu QR kod heç bir masaya aid deyil. Zəhmət olmasa masadakı QR kodu yenidən skan edin.',
    errorDescription: 'İnternet bağlantınızı yoxlayıb yenidən cəhd edin.',
    retry: 'Yenidən cəhd et',
    noOpenBill:
      'Bu masada hələ açıq hesab yoxdur. Sifariş verildikdən sonra burada görünəcək.',
  },
  confirmOrder: {
    title: 'Əminsiniz?',
    description: 'Bu məhsullar masanın hesabına əlavə olunacaq.',
    total: 'Cəmi',
    warning: 'Təsdiqdən sonra sifarişi silmək olmur.',
    back: 'Geri qayıt',
    sending: 'Göndərilir...',
    confirm: 'Bəli, təsdiqlə',
  },
  dish: {
    add: 'Əlavə et',
    addAria: '{{name}} əlavə et',
    decreaseAria: '{{name}} azalt',
    quantityAria: '{{name}} sayı',
    increaseAria: '{{name}} artır',
    aboutAria: '{{name}} haqqında',
    combo: 'Kombo',
    ingredients: 'Əsas tərkib',
  },
  menuBrowser: {
    title: 'Menyu',
    description:
      'Kateqoriya seçin, yeməkləri əlavə edin, sonra sifarişi təsdiqləyin.',
    loading: 'Yüklənir',
    failed: 'Menyunu yükləmək alınmadı.',
    empty: 'Bu gün menyuda sifariş ediləcək məhsul yoxdur.',
    categories: 'Kateqoriyalar',
    picked: 'Seçilənlər',
    nothingPicked: 'Hələ heç nə seçilməyib.',
    decreasePickedAria: 'Seçilən {{name}} azalt',
    increasePickedAria: 'Seçilən {{name}} artır',
    removeAria: '{{name}} səbətdən çıxar',
    subtotal: 'Ara cəm',
    confirm: 'Sifarişi təsdiqlə ({{amount}})',
  },
  menuOrder: {
    restaurantClosed:
      'Restoran hazırda onlayn sifariş qəbul etmir. Zəhmət olmasa ofisianta müraciət edin.',
    conflict:
      'Bəzi məhsullar artıq mövcud deyil. Menyu yeniləndi, səbəti yoxlayın.',
    failed: 'Sifarişi göndərmək alınmadı. Yenidən cəhd edin.',
    title: 'Menyu və sifariş',
    description:
      'Seçin, yoxlayın və təsdiqləyin. Sifariş masanın hesabına düşür.',
    cancelHint:
      'Təsdiqlənmiş sifarişi ləğv etmək üçün restoran menecerinə müraciət edin.',
    newRound: 'Əlavə sifariş',
    ordered: 'Sifarişiniz masanın hesabına əlavə olundu.',
    menu: 'Menyu',
    basket: 'Səbət',
    removeAria: '{{name}} səbətdən çıxar',
    confirm: 'Sifarişi təsdiqlə ({{amount}})',
  },
  upsell: {
    askOne: '{{name}} istəyirsiniz?',
    askDefault: 'Əlavə',
    askMany: '{{names}} və ya {{last}} istəyirsiniz?',
    title: 'Bunu da istərdiniz?',
    askedDescription: 'İstəsəniz sayını seçin, sifarişə əlavə olunacaq.',
    description: 'Seçdiyiniz məhsullarla yaxşı gedir.',
    addedAria: '{{name}} əlavə olundu',
    addAria: '{{name}} əlavə et',
    added: 'Əlavə olundu',
    add: 'Əlavə et',
    free: 'Pulsuz',
    decreaseAria: '{{name}} azalt',
    quantityAria: '{{name}} sayı',
    increaseAria: '{{name}} artır',
    continue: 'Davam et',
    noThanks: 'Xeyr, təşəkkürlər',
  },
  tablePage: {
    payment: 'Ödəniş',
  },
  invalidToken: {
    missingTitle: 'QR kod tanınmadı',
    missingDescription:
      'Bu keçid masa məlumatını daşımır. Zəhmət olmasa masadakı QR kodu yenidən skan edin.',
    malformedTitle: 'Keçid etibarsızdır',
    malformedDescription:
      'QR kod düzgün oxunmadı. Zəhmət olmasa masadakı QR kodu yenidən skan edin.',
    help: 'Kömək lazımdırsa restoran işçisi ilə əlaqə saxlayın.',
  },
  notFound: {
    title: 'Səhifə tapılmadı',
    description:
      'Axtardığınız səhifə mövcud deyil. Masadakı QR kodu skan edərək başlayın.',
  },
}
