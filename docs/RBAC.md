# RBAC — Rollar və İcazə Matrisi

masapay üç rol istifadə edir. Rollar `users.role` sütununda saxlanılır və giriş
zamanı JWT access token-ə `role` claim-i kimi yazılır; hər sorğuda
`JwtAuthenticationFilter` bunu `ROLE_<rol>` authority-sinə çevirir. Endpoint-lər
`@PreAuthorize` ilə qorunur (`@EnableMethodSecurity`).

## Rollar

- **SUPER_ADMIN** — platforma sahibi. Bütün restoranları, istifadəçiləri və
  qlobal tənzimləmələri idarə edir.
- **RESTO_MANAGER** — bir restoranın meneceri. Yalnız öz restoranını idarə edir
  (heyət, masalar, adisyonlar, ödəniş/POS tənzimləmələri, refund, analitika).
- **WAITER** — restoran işçisi. Öz restoranının masalarına və adisyonlarına
  xidmət edir; idarəetmə icazəsi yoxdur.

Yeni Google istifadəçisi `enabled=false` və rolsuz yaradılır; SUPER_ADMIN onu
aktivləşdirib rol təyin edənə qədər heç bir icazəsi yoxdur. İlk SUPER_ADMIN
`masapay.auth.super-admin-emails` konfiqurasiyası ilə bootstrap olunur.

## İcazə matrisi

| Resurs / Əməliyyat | SUPER_ADMIN | RESTO_MANAGER | WAITER | Public |
|---|:---:|:---:|:---:|:---:|
| `GET /actuator/health`, `/info` | - | - | - | bəli |
| `POST /api/auth/google`, `POST /api/auth/refresh` | - | - | - | bəli |
| `GET /api/me` (öz profili) | bəli | bəli | bəli | xeyr |
| `GET /api/admin/users` (istifadəçi siyahısı) | bəli | xeyr | xeyr | xeyr |
| `PATCH /api/admin/users/{id}` (aktivləşdir / rol təyini) | bəli | xeyr | xeyr | xeyr |

Yuxarıdakı sətirlər hazırda tətbiq olunan (enforced) endpoint-lərdir. Aşağıdakı
resurslar müvafiq EPIC-lərdə əlavə olunacaq və bu siyasətə uyğun qorunacaq:

| Resurs / Əməliyyat (planlaşdırılan) | SUPER_ADMIN | RESTO_MANAGER | WAITER |
|---|:---:|:---:|:---:|
| Restoran tənzimləmələri (POS/Payriff) | bütün | öz restoranı | xeyr |
| Heyət idarəetməsi | bütün | öz restoranı | xeyr |
| Masa / QR idarəetməsi | bütün | öz restoranı | xeyr |
| Canlı adisyonlar və floor plan | bütün | öz restoranı | öz restoranı |
| Adisyona xidmət / masanı bağlama | bütün | öz restoranı | öz restoranı |
| Refund | bütün | öz restoranı | xeyr |
| Analitika / hesabatlar | bütün | öz restoranı | xeyr |

## Cavab kodları

- Autentifikasiya yoxdursa (token yox / vaxtı bitib / etibarsız): **401**
  (`RestAuthenticationEntryPoint`).
- Autentifikasiya var, lakin rol icazəsi yoxdursa: **403** (`@PreAuthorize` rədd
  edir).
