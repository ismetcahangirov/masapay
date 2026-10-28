// Azerbaijani texts of the "auth" namespace: the source the other languages follow.
export default {
  login: {
    title: 'masapay admin',
    subtitle: 'Google hesabınızla daxil olun',
    notConfigured:
      'Google girişi konfiqurasiya olunmayıb: frontend/.env.local faylında VITE_GOOGLE_CLIENT_ID təyin edin.',
    checking: 'Yoxlanılır...',
    pending:
      'Hesabınız yaradıldı və restoran menecerinin və ya administratorun təsdiqini gözləyir. Təsdiqdən sonra yenidən daxil olun.',
    failed: 'Giriş alınmadı. Yenidən cəhd edin.',
  },
  devLogin: {
    title: 'Lokal test girişi (Google-suz)',
    admin: 'Test admini (SUPER_ADMIN)',
    manager: 'Test meneceri (Dolma Restoran)',
    waiter: 'Test ofisiantı (Dolma Restoran)',
    chef: 'Test aşpazı (Dolma Restoran)',
    failed:
      'Test girişi alınmadı. Backend MASAPAY_DEV_LOGIN_ENABLED=true ilə işə salınmalıdır.',
  },
  logout: 'Çıxış',
  guard: {
    checkingSession: 'Sessiya yoxlanılır',
    noAccessTitle: 'Giriş icazəniz yoxdur',
    noAccessBody:
      'Bu bölmə yalnız restoran menecerləri və administratorlar üçündür.',
  },
}
