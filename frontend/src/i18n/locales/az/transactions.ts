// Azerbaijani texts of the "transactions" namespace: the source the other languages follow.
export default {
  status: {
    PENDING: 'Gözləyir',
    SUCCESS: 'Uğurlu',
    FAILED: 'Uğursuz',
    REFUNDED: 'Geri qaytarılıb',
  },
  split: {
    FULL: 'Tam',
    ITEM: 'Məhsula görə',
    EQUAL: 'Bərabər',
  },
  files: {
    excel: 'emeliyyatlar.xlsx',
  },
  refund: {
    title: 'Ödənişi geri qaytar',
    description:
      '{{table}}: {{amount}} müştərinin kartına tam qaytarılacaq. Bu addımı geri almaq olmur.',
    reason: 'Səbəb (istəyə bağlı)',
    reasonPlaceholder: 'Məsələn, səhv məbləğ ödənilib',
    cancel: 'İmtina',
    refunding: 'Qaytarılır...',
    refund: 'Geri qaytar',
    errors: {
      windowExpired:
        'Ödənişdən 30 gündən çox keçib: Payriff pulu geri qaytarmağa icazə vermir. Ödəniş olduğu kimi qalır.',
      refused: 'Payriff geri qaytarmanı rədd etdi. Ödəniş olduğu kimi qalır.',
      unavailable:
        'Payriff hazırda cavab vermir. Bir az sonra yenidən cəhd edin.',
      generic: 'Geri qaytarmaq alınmadı. Yenidən cəhd edin.',
    },
  },
  page: {
    title: 'Əməliyyatlar',
    subtitle:
      'Bütün Payriff ödənişləri. Uğurlu ödənişi buradan geri qaytara bilərsiniz.',
    preparing: 'Hazırlanır...',
    downloadExcel: 'Excel yüklə',
    excelFailed: 'Excel yükləmək alınmadı.',
    noRestaurant: 'Hesabınıza hələ restoran təyin olunmayıb.',
    filters: 'Filtrlər',
    searchPlaceholder: 'Payriff sifariş ID, əməliyyat ID və ya masa',
    search: 'Axtarış',
    from: 'Başlanğıc',
    to: 'Son',
    status: 'Status',
    allStatuses: 'Bütün statuslar',
    splitType: 'Ödəniş tipi',
    allSplitTypes: 'Bütün tiplər',
    clearFilters: 'Filtrləri sıfırla',
    loadFailed: 'Əməliyyatları yükləmək alınmadı.',
    date: 'Tarix',
    table: 'Masa',
    amount: 'Məbləğ',
    tip: 'Bəxşiş',
    total: 'Cəmi',
    payriffId: 'Payriff ID',
    action: 'Əməliyyat',
    refund: 'Geri qaytar',
    empty: 'Heç bir əməliyyat tapılmadı.',
    loading: 'Yüklənir',
    previousPage: 'Əvvəlki səhifə',
    nextPage: 'Növbəti səhifə',
  },
}
