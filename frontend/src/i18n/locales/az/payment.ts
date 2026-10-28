// Azerbaijani texts of the "payment" namespace: the source the other languages follow.
export default {
  actions: {
    full: 'Tam ödə',
    equal: 'Bərabər böl',
    items: 'Öz yediyim',
    itemsLockedTitle: 'Hesab artıq bərabər bölünür',
    itemsTitle: 'Yalnız öz məhsullarınızı ödəyin',
    splitStartedNote:
      'Hesab bərabər bölünür, ona görə "Öz yediyim" bağlıdır. Qalan hissəni "Bərabər böl" və ya "Tam ödə" ilə ödəyin.',
  },
  pay: {
    redirecting: 'Payriff-ə yönləndirilir...',
    payAmount: '{{amount}} ödə',
    pay: 'Ödə',
  },
  full: {
    title: 'Tam ödə',
    description: 'Hesabın qalan hissəsinin hamısını siz ödəyirsiniz.',
  },
  equal: {
    title: 'Bərabər böl',
    started:
      'Hesab {{total}} bərabər hissəyə bölünüb, {{paid}} hissə ödənilib.',
    ask: 'Hesabı neçə nəfər arasında bölmək istəyirsiniz?',
    people: 'Nəfər sayı',
    partsToPay: 'Neçə nəfər üçün ödəyirsiniz',
    startedHint:
      'Bölgü başlayıb, ona görə nəfər sayı dəyişmir. {{remaining}} hissə qalıb, ən çox {{remaining}} nəfər üçün ödəyə bilərsiniz.',
    hint: 'Başqaları üçün də ödəyirsinizsə, sayı artırın (ən çox {{remaining}}).',
  },
  items: {
    title: 'Öz yediyini ödə',
    description: 'Yalnız sizin sifariş etdiyiniz məhsulları seçin.',
    paid: 'Ödənilib',
    guests: 'Masada neçə nəfərsiniz',
    pickOne: 'Ödəmək üçün ən azı bir məhsul seçin.',
  },
  offer: {
    title: 'Bu məhsulları da istəyirsiniz?',
    description:
      'Ödəməzdən əvvəl: əlavə etsəniz hesaba düşür və ödənişə daxil olur.',
    addedLabel: '{{name}} əlavə olundu',
    addLabel: '{{name}} əlavə et',
    added: 'Əlavə olundu',
    add: 'Əlavə et',
    addFailed: 'Əlavə etmək alınmadı. Məhsul artıq mövcud olmaya bilər.',
    goToPayment: 'Ödənişə keç',
    noPay: 'Xeyr, ödə',
  },
  parts: {
    decrease: '{{label}}: azalt',
    increase: '{{label}}: artır',
    amountDue: 'Ödəniləcək',
  },
  tip: {
    title: 'Bəxşiş',
    tooBig: 'Bəxşiş ödənilən hissədən çox ola bilməz.',
    total: 'Cəmi ödəniş',
    custom: 'Başqa məbləğ',
    customLabel: 'Bəxşiş məbləği (AZN)',
    invalid: 'Düzgün məbləğ daxil edin.',
    tip: 'Bəxşiş:',
  },
  errors: {
    offline:
      'İnternet bağlantısı yoxdur. Bağlantını yoxlayıb yenidən cəhd edin, pul çəkilməyib.',
    payriffUnavailable:
      'Ödəniş sistemi hazırda cavab vermir, pul çəkilməyib. Bir az sonra yenidən cəhd edin.',
    noOnlinePayments:
      'Restoran hazırda onlayn ödəniş qəbul etmir. Zəhmət olmasa ofisianta müraciət edin.',
    conflict:
      'Hesab bu arada dəyişdi (kimsə artıq ödəmiş ola bilər). Məbləği yoxlayıb yenidən cəhd edin.',
    badChoice: 'Seçim yanlışdır. Zəhmət olmasa yenidən seçin.',
    generic:
      'Ödəniş alınmadı. İnternet bağlantınızı yoxlayıb yenidən cəhd edin.',
  },
  review: {
    title: 'Təcrübənizi qiymətləndirin',
    textNamed:
      '{{name}} bəyəndinizsə, Google-da 5 ulduzlu rəy bizə çox kömək edər.',
    text: 'Bəyəndinizsə, Google-da 5 ulduzlu rəy bizə çox kömək edər.',
    rate: 'Google-da qiymətləndir',
    later: 'Sonra',
  },
  result: {
    checking: {
      title: 'Ödəniş yoxlanılır',
      text: 'Payriff-dən təsdiq gözlənilir. Bu bir neçə saniyə çəkə bilər, səhifəni bağlamayın.',
    },
    success: {
      title: 'Ödəniş uğurla tamamlandı',
      text: 'Təşəkkür edirik! Hesabınız yeniləndi.',
    },
    failed: {
      title: 'Ödəniş baş tutmadı',
      text: 'Kartınızdan pul çəkilmədi. Masaya qayıdıb yenidən cəhd edə bilərsiniz.',
    },
    slow: {
      title: 'Təsdiq gecikir',
      text: 'Payriff hələ cavab verməyib. Ödəniş tamamlanıbsa, hesab bir azdan yenilənəcək. Bir neçə dəqiqədən sonra yenidən yoxlayın.',
    },
    offline: {
      title: 'İnternet bağlantısı kəsildi',
      text: 'Ödənişin vəziyyətini yoxlaya bilmədik. Bağlantını bərpa edib yenidən yoxlayın, ödənişiniz itməyib.',
    },
    unknown: {
      title: 'Ödəniş tapılmadı',
      text: 'Bu keçid etibarsızdır. Masadakı QR kodu yenidən skan edin.',
    },
    amount: 'Məbləğ',
    retry: 'Yenidən yoxla',
    back: 'Hesaba qayıt',
  },
  mock: {
    badge: 'Test ödəniş səhifəsi (Payriff əvəzi)',
    description: 'Real kart tələb olunmur. Nəticəni seçin.',
    approve: 'Ödə (uğurlu)',
    decline: 'İmtina et (uğursuz)',
    failed:
      'Cavab göndərilmədi. Backend MASAPAY_PAYRIFF_MOCK=true ilə işə salınmalıdır.',
  },
}
