# masapay

Restoranlar üçün QR-əsaslı, real-time adisyon və hesab-bölmə + ödəniş platforması
(Sunday App klonu). Müştəri masadakı QR kodu skan edir, canlı adisyonu görür,
hesabı bölür, bəxşiş əlavə edir və Payriff vasitəsilə ödəyir; sistem restoranın
POS-u (iiko/Micros/Custom) ilə iki-tərəfli sinxronlaşır.

## Layihə Qaydaları (MƏCBURİ)

Bütün development işi bu qaydalara tabedir. Bu qaydalar hər GitHub issue-nun
sonuna footer kimi əlavə olunub:

- **Emoji QADAĞANDIR** — issue mətnlərində, commit mesajlarında, UI
  komponentlərində və sənədləşdirmədə. Vizual işarələr üçün YALNIZ ikon
  komponentləri (`lucide-react`) istifadə olunur.
- **Əsas UI komponent kitabxanası: shadcn/ui** (Radix UI əsasında). Yeni komponent
  lazım olduqda əvvəlcə shadcn-in mövcud komponentindən istifadə et; yalnız
  qarşılığı olmadıqda custom komponent yaz.
- **Qradient rənglər və Bənövşəyi-Mavi (#6366F1, #8B5CF6) qarışıqları qadağandır.**
- **Rəng palitrası:**
  - Fon: `#FAFAFA` / `#FFFFFF`
  - Mətn / Düymə: `#0F172A` / `#000000`
  - Vurğu (uğur / CTA): `#10B981` / `#059669`
  - Border: `#E2E8F0`

## Texnoloji Stack

**Frontend** (`/frontend`)

- React 18 + Vite + TypeScript (strict mode)
- TailwindCSS + shadcn/ui + lucide-react
- Redux Toolkit + RTK Query
- React Router, `@react-oauth/google`
- Vitest + React Testing Library

**Backend** (`/backend`)

- Java 21 + Spring Boot 3.x (Web, Data JPA, Security, WebSocket, Validation)
- PostgreSQL 16 + Flyway miqrasiyaları
- Redis (session cache + WebSocket pub/sub)
- Spring Security OAuth2 (Google) + JWT (access/refresh)
- WebSocket STOMP real-time sinxronizasiya
- JUnit 5 + Testcontainers

**İnteqrasiyalar**

- Payriff API v2 (ödəniş, HMAC webhook, refund)
- POS: iiko / Micros / Custom (`POSAdapter` interfeysi)

**DevOps**

- Docker Compose (postgres, redis, backend, frontend)
- GitHub Actions CI/CD

## Repo Strukturu

```
masapay/
├── frontend/     # React 18 + Vite + TS PWA və admin panel
├── backend/      # Spring Boot 3.x API
├── docker-compose.yml
├── BACKLOG.md    # Epic / sub-issue backlog və GitHub issue əlaqələri
└── CLAUDE.md
```

## Verilənlər Bazası (əsas cədvəllər)

`restaurants`, `tables`, `orders`, `order_items`, `transactions`

## Rollar (RBAC)

`SUPER_ADMIN`, `RESTO_MANAGER`, `WAITER` — `@PreAuthorize` ilə endpoint-lər qorunur.

## İş Bölgüsü

- **Frontend** issue-ları → `@ismetcahangirov`
- **Backend** issue-ları → `@YunisPashayev`
- Həm frontend həm backend olan issue-lar → hər ikisi assign olunub.

Tam backlog və issue nömrələri üçün bax: [BACKLOG.md](./BACKLOG.md).

## Git / GitHub Workflow (qısa)

- İşə başlamazdan əvvəl `main` pull et; hər tapşırıq üçün ayrıca branch (`feature/...`, `fix/...`, `chore/...`, `docs/...`).
- **Bir tapşırığı bir nəhəng commit ilə atma — işi məntiqi, kiçik hissələrə bölüb ayrı-ayrı commit-lər et.** Hər commit tək bir tamamlanmış dəyişikliyi əhatə etsin (məs. əvvəl schema/migration, sonra API/endpoint, sonra UI komponenti, sonra testlər). Hər commit özlüyündə build/test-dən keçən vəziyyətdə olsun və mesajı ingiliscə, Conventional Commits formatında yazılsın (`feat:`, `fix:`, `chore:` və s.).
- PR **ingiliscə** yazılır, label-lar əlavə olunur, repo sahibinə assign edilir; CI keçəndən sonra merge.
- Hər EPIC üçün label: `epic:auth`, `epic:home`, `epic:admin` və s.
- Ətraflı: `git-workflow` skill.
