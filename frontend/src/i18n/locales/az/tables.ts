// Azerbaijani texts of the "tables" namespace: the source the other languages follow.
export default {
  page: {
    title: 'Masalar',
    subtitle:
      'Hər masanın öz QR kodu var. Müştəri onu skan edib hesabı görür və ödəyir.',
    preparing: 'Hazırlanır...',
    downloadAll: 'Hamısını yüklə (ZIP)',
    zipFileName: '{{name}}-qr-kodlar.zip',
    zipFallbackName: 'masalar',
    newTablePlaceholder: 'Yeni masanın adı, məsələn Masa 7',
    newTableLabel: 'Yeni masanın adı',
    add: 'Əlavə et',
    duplicate: 'Bu adda masa artıq var.',
    createFailed: 'Masanı əlavə etmək alınmadı.',
    zipFailed: 'ZIP yükləmək alınmadı.',
    loading: 'Yüklənir',
    loadFailed: 'Masaları yükləmək alınmadı.',
    empty: 'Hələ masa yoxdur.',
    emptyHint: ' Yuxarıdan ilk masanı əlavə edin.',
  },
  qrCard: {
    qrAlt: '{{label}} QR kodu',
    open: 'Aç',
    serviceCharge: 'Xidmət haqqı: {{value}}',
    editLabel: '{{label}} xidmət haqqı',
    edit: 'Dəyiş',
  },
  serviceCharge: {
    none: 'Yoxdur',
    restaurant: '{{percent}} (restoran)',
    thisTable: '{{percent}} (bu masa)',
  },
  serviceChargeDialog: {
    title: 'Xidmət haqqı',
    description:
      '{{label}}: bu masanın hesablarına nə qədər xidmət haqqı gəlsin. Yeni sətir əlavə olunanda tətbiq olunur.',
    restaurant: 'Restoranınkı',
    own: 'Xüsusi faiz',
    none: 'Yoxdur',
    percentLabel: 'Faiz (%)',
    percentInvalid:
      '0-dan böyük, 100-dən kiçik rəqəm yazın, məsələn 15 və ya 12,5',
    saveFailed: 'Yadda saxlamaq alınmadı.',
    cancel: 'İmtina',
    saving: 'Yadda saxlanılır...',
    save: 'Yadda saxla',
  },
}
