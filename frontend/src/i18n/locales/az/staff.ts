// Azerbaijani texts of the "staff" namespace: the source the other languages follow.
export default {
  roles: {
    WAITER: 'Ofisiant',
    CHEF: 'Aşpaz',
  },
  status: {
    INVITED: 'Dəvət olunub, hələ daxil olmayıb',
    PENDING: 'Təsdiq gözləyir',
    ACTIVE: 'Aktiv',
    DEACTIVATED: 'Deaktiv',
  },
  page: {
    title: 'İşçilər',
    description:
      'Ofisiant və aşpazları Google hesablarının emaili ilə dəvət edin. Onlar Google ilə daxil olandan sonra burada təsdiqləyin.',
    inviteForm: 'İşçi dəvət et',
    email: 'Google emaili',
    emailPlaceholder: 'ad.soyad@gmail.com',
    role: 'Rol',
    invite: 'Dəvət et',
    emailTaken: 'Bu email artıq bir işçiyə və ya başqa restorana aiddir.',
    inviteFailed: 'Dəvət etmək alınmadı. Emaili yoxlayın.',
    updateFailed: 'Dəyişiklik alınmadı.',
    loading: 'Yüklənir',
    loadFailed: 'İşçiləri yükləmək alınmadı.',
    columns: {
      member: 'İşçi',
      role: 'Rol',
      status: 'Vəziyyət',
      actions: 'Əməliyyat',
    },
    roleAria: '{{email}} rolu',
    approveAria: '{{email}} təsdiqlə',
    approve: 'Təsdiqlə',
    activate: 'Aktiv et',
    deactivateAria: '{{email}} deaktiv et',
    deactivate: 'Deaktiv et',
    empty: 'Hələ işçi yoxdur. Yuxarıdan ilk ofisiant və ya aşpazı dəvət edin.',
  },
}
