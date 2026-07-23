# masapay

Restoranlar üçün QR-əsaslı, real-time adisyon və hesab-bölmə + ödəniş platforması.
Müştəri masadakı QR kodu skan edir, canlı adisyonu görür, hesabı bölür, bəxşiş
əlavə edir və Payriff vasitəsilə ödəyir; sistem restoranın POS-u (iiko / Micros /
Custom) ilə iki-tərəfli sinxronlaşır.

## Monorepo Strukturu

```
masapay/
├── frontend/            # React 18 + Vite + TypeScript PWA və admin panel
├── backend/             # Spring Boot 3.x REST API
├── docker-compose.yml   # postgres, redis, backend, frontend (sonrakı epic-lər)
├── BACKLOG.md           # Epic / sub-issue backlog
├── CLAUDE.md            # Layihə qaydaları və texnoloji stack
└── README.md
```

`frontend/` və `backend/` bir-birindən müstəqil workspace-lərdir; hər biri öz
asılılıqlarını və build alətlərini idarə edir (frontend üçün npm, backend üçün
Maven).

## Texnoloji Stack

- **Frontend:** React 18, Vite, TypeScript (strict), TailwindCSS, shadcn/ui,
  lucide-react, Redux Toolkit + RTK Query, Vitest.
- **Backend:** Java 21, Spring Boot 3.x, PostgreSQL 16 + Flyway, Redis, Spring
  Security (OAuth2 + JWT), WebSocket STOMP, JUnit 5 + Testcontainers.
- **İnteqrasiyalar:** Payriff API v2, POS adapterləri (iiko / Micros / Custom).
- **DevOps:** Docker Compose, GitHub Actions.

## Development

Hər workspace müstəqil işə salınır:

```bash
# Frontend
cd frontend
npm install
npm run dev

# Backend
cd backend
./mvnw spring-boot:run
```

## Layihə Qaydaları

Tam qaydalar üçün [CLAUDE.md](./CLAUDE.md); backlog və issue əlaqələri üçün
[BACKLOG.md](./BACKLOG.md).

- UI-da emoji istifadəsi qadağandır; vizual işarələr yalnız `lucide-react`
  ikonları ilə verilir.
- Əsas UI komponent kitabxanası shadcn/ui-dir.
- Qradient və bənövşəyi-mavi (#6366F1 / #8B5CF6) rənglər qadağandır.
