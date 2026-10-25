# masapay

Restoranlar üçün QR-əsaslı, real-time adisyon və hesab-bölmə + ödəniş platforması.
Müştəri masadakı QR kodu skan edir, canlı adisyonu görür, hesabı bölür, bəxşiş
əlavə edir və Payriff vasitəsilə ödəyir; sistem restoranın POS-u (iiko / Micros /
Custom) ilə iki-tərəfli sinxronlaşır.

## Mündəricat

- [Monorepo strukturu](#monorepo-strukturu)
- [Texnoloji stack](#texnoloji-stack)
- [Lokal qaldırma](#lokal-qaldırma)
- [Testlər və CI](#testlər-və-ci)
- [Environment dəyişənləri](#environment-dəyişənləri)
- [Production deployment](#production-deployment)
- [Layihə qaydaları](#layihə-qaydaları)

## Monorepo strukturu

```
masapay/
├── frontend/            # React 18 + Vite + TypeScript: müştəri ekranı və admin panel
├── backend/             # Spring Boot 3.x REST API + WebSocket (Gradle)
├── scripts/db/          # lokal baza: rol/baza yaratma və mock data
├── .github/workflows/   # CI (GitHub Actions)
├── settings.gradle      # kök Gradle build, backend/-i daxil edir
├── docker-compose.yml   # lokal tam stack: postgres, redis, backend, frontend
├── BACKLOG.md           # Epic / sub-issue backlog
├── CLAUDE.md            # Layihə qaydaları
└── docs/                # DESIGN.md (dizayn dili), RBAC.md (rollar), brand/
```

`frontend/` və `backend/` müstəqil workspace-lərdir: frontend npm, backend Gradle
ilə idarə olunur.

## Texnoloji stack

- **Frontend:** React 18, Vite, TypeScript (strict), TailwindCSS, shadcn/ui,
  lucide-react, Redux Toolkit + RTK Query, Vitest.
- **Backend:** Java 21, Spring Boot 3.x, PostgreSQL 16 + Flyway, Redis, Spring
  Security (Google OAuth2 + JWT), WebSocket STOMP, JUnit 5 + Testcontainers.
- **İnteqrasiyalar:** Payriff API, POS adapterləri (iiko / Micros / Custom).
- **DevOps:** Docker, Docker Compose, GitHub Actions.

## Lokal qaldırma

Tələblər:

| Alət | Versiya | Qeyd |
|---|---|---|
| JDK | 21 | backend |
| Node.js | 22 | frontend (CI də 22 işlədir) |
| PostgreSQL | 16 | Docker-siz yolda maşına quraşdırılır |
| Redis | 7 | istəyə bağlı: lokalda onsuz da işləyir |
| Docker | istənilən təzə versiya | yalnız A yolu və Testcontainers testləri üçün |

### A yolu: Docker Compose ilə hər şey bir əmrlə

```bash
docker compose up -d --build
```

- Frontend: http://localhost:3000 (nginx, `/api` və `/ws` backend-ə ötürülür)
- Backend: http://localhost:8080
- PostgreSQL host-da `5433`, Redis `6379` portundadır.

Compose lokal üçündür: açıq dev secret-lərə icazə verir (`MASAPAY_ALLOW_DEV_SECRETS=true`).
Production üçün aşağıdakı [Production deployment](#production-deployment) bölməsinə bax.

### B yolu: Docker-siz (PostgreSQL maşında)

1. Rol və baza (`masapay` / `masapay`), `postgres` superuser ilə:

   ```bash
   psql -U postgres -h localhost -f scripts/db/1-setup-db.sql
   ```

2. Backend-i dev rejimində işə sal (Flyway sxemi özü yaradır):

   ```bash
   cd backend
   MASAPAY_REDIS_ENABLED=false MASAPAY_DEV_LOGIN_ENABLED=true MASAPAY_PAYRIFF_MOCK=true ./gradlew bootRun
   ```

   PowerShell:

   ```powershell
   $env:MASAPAY_REDIS_ENABLED="false"; $env:MASAPAY_DEV_LOGIN_ENABLED="true"; $env:MASAPAY_PAYRIFF_MOCK="true"
   .\gradlew.bat bootRun
   ```

   - `MASAPAY_REDIS_ENABLED=false`: Redis-siz işləyir. Canlı yeniləmələr
     (WebSocket) yenə işləyir, sadəcə bir nüsxə daxilində.
   - `MASAPAY_DEV_LOGIN_ENABLED=true`: Google-suz test girişi və dev secret-lərə icazə.
     **Yalnız lokal.**
   - `MASAPAY_PAYRIFF_MOCK=true`: real Payriff əvəzinə lokal ödəniş səhifəsi, pul çəkilmir.

3. Mock data (restoranlar, masalar, açıq hesablar, test istifadəçiləri; menyunu
   menecer kimi admin paneldə, "Menyu" səhifəsində əlavə et):

   ```bash
   psql -U masapay -h localhost -d masapay -f scripts/db/2-seed-mock-data.sql
   ```

4. Frontend:

   ```bash
   cd frontend
   npm ci
   npm run dev
   ```

   http://localhost:5173 açılır; `/api` və `/ws` avtomatik `localhost:8080`-ə ötürülür.

5. Giriş: http://localhost:5173/admin/login səhifəsində test düymələri var:
   super admin, menecer, ofisiant, aşpaz. Müştəri ekranı (Dolma, Masa 4):
   http://localhost:5173/r/a0000000-0000-0000-0000-000000000001/t/b0000000-0000-0000-0000-000000000004?token=11111111-0000-0000-0000-000000000004

Backend default olaraq PostgreSQL-i `localhost:5432`-də gözləyir. Compose-dakı
PostgreSQL-ə qoşulursansa (`5433`):

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/masapay
```

Repo kökündən də işə salmaq olar (`./gradlew bootRun`): kök Gradle build `backend/`-i
daxil edir.

## Testlər və CI

```bash
cd backend && ./gradlew check        # unit + inteqrasiya testləri
cd frontend && npm test              # vitest
cd frontend && npm run lint && npm run lint:css && npm run format:check && npm run build
node scripts/check-no-emoji.mjs $(git ls-files)   # emoji qadağası
```

Testcontainers testləri (PostgreSQL, Redis) yalnız Docker olanda işləyir, onsuz
avtomatik keçilir.

Hər pull request və `main`-ə hər push üçün GitHub Actions
([`.github/workflows/ci.yml`](./.github/workflows/ci.yml)) yuxarıdakıların hamısını
işlədir və hər iki Docker image-in build olduğunu yoxlayır. PR yalnız CI yaşıl olanda
merge olunur.

## Environment dəyişənləri

Backend-in bütün ayarları [`application.yml`](./backend/src/main/resources/application.yml)-dədir;
hər birini eyni adlı environment dəyişəni əvəz edir. Production üçün vacib olanlar:

### Mütləq (production-da default ilə backend başlamır)

| Dəyişən | Nə üçün | Necə yaradılır |
|---|---|---|
| `MASAPAY_JWT_SECRET` | access / refresh token imzası | `openssl rand -base64 48` |
| `MASAPAY_PAYRIFF_WEBHOOK_SECRET` | Payriff callback-in həqiqiliyi | `openssl rand -base64 48` |
| `MASAPAY_SECRETS_ENCRYPTION_KEY` | restoranların Payriff / iiko açarlarının DB-də şifrələnməsi (AES-256-GCM) | `openssl rand -base64 32` |
| `SPRING_DATASOURCE_URL` | PostgreSQL ünvanı | `jdbc:postgresql://<host>:5432/masapay` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | DB istifadəçisi | güclü parol; compose-dakı `masapay` yalnız lokal üçündür |
| `SPRING_PROFILES_ACTIVE` | production profili | `prod` |

### İnteqrasiyalar və ünvanlar

| Dəyişən | Default | Qeyd |
|---|---|---|
| `MASAPAY_GOOGLE_CLIENT_ID` | boş | Google OAuth client id (backend tokeni yoxlayır) |
| `MASAPAY_SUPER_ADMIN_EMAILS` | boş | vergüllə: Google ilə ilk girişdə SUPER_ADMIN olan emaillər |
| `MASAPAY_PAYRIFF_APP_KEY` | boş | Payriff merchant app key |
| `MASAPAY_PAYRIFF_BASE_URL` | `https://api.payriff.com` | |
| `MASAPAY_PAYRIFF_CALLBACK_URL` | `http://localhost:8080/api/public/payriff/webhook` | public HTTPS ünvan olmalıdır |
| `MASAPAY_PAYRIFF_REDIRECT_URL` | `http://localhost:5173/payment/result` | ödənişdən sonra müştərinin qayıtdığı səhifə |
| `MASAPAY_QR_BASE_URL` | `http://localhost:5173` | QR kodların içindəki saytın ünvanı |
| `MASAPAY_IIKO_BASE_URL` | `https://api-ru.iiko.services` | |
| `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` | `localhost` / `6379` | |
| `MASAPAY_PAYRIFF_RECONCILE_INTERVAL` / `..._MIN_AGE` / `..._GIVE_UP_AFTER` | `PT1M` / `PT10M` / `P2D` | Payriff-in callback göndərmədiyi PENDING ödənişlər bu qrafiklə Payriff-dən yoxlanılır və yekunlaşdırılır (müştəri ödəniş səhifəsini tərk edib, callback itib). `MASAPAY_PAYRIFF_RECONCILE_ENABLED=false` söndürür; mock rejimdə özü işləmir |
| `MASAPAY_REDIS_ENABLED` | `true` | birdən çox nüsxədə mütləq `true` (canlı yeniləmələr və rate limit nüsxələr arası paylaşılır) |

### Təhlükəsizlik və limitlər

| Dəyişən | Default | Qeyd |
|---|---|---|
| `MASAPAY_SECRETS_KEY_ID` / `MASAPAY_SECRETS_OLD_KEYS` | `k1` / boş | açar rotasiyası, aşağıya bax |
| `MASAPAY_RATE_LIMIT_ENABLED` | `true` | |
| `MASAPAY_RATE_LIMIT_AUTH_PER_MINUTE` | `20` | IP üzrə |
| `MASAPAY_RATE_LIMIT_PAYMENT_PER_MINUTE` / `..._PER_DAY` | `10` / `100` | IP üzrə |
| `MASAPAY_RATE_LIMIT_PUBLIC_PER_MINUTE` | `120` | IP üzrə |
| `MASAPAY_RATE_LIMIT_STAFF_PER_MINUTE` | `300` | istifadəçi üzrə |
| `MASAPAY_JWT_ACCESS_TTL` / `MASAPAY_JWT_REFRESH_TTL` | `PT15M` / `P7D` | |

**Yalnız lokal** (production-da təyin olunmur): `MASAPAY_DEV_LOGIN_ENABLED`,
`MASAPAY_ALLOW_DEV_SECRETS`, `MASAPAY_PAYRIFF_MOCK`, `MASAPAY_API_DOCS_ENABLED`.

### Frontend (build zamanı)

| Dəyişən | Qeyd |
|---|---|
| `VITE_GOOGLE_CLIENT_ID` | Google giriş düyməsi. Vite onu build zamanı bundle-a yazır: lokalda `frontend/.env.local`-da, Docker-də `--build-arg` ilə verilir. |

## Production deployment

### 1. Image-lər

```bash
docker build -t masapay-backend:<versiya> backend
docker build -t masapay-frontend:<versiya> --build-arg VITE_GOOGLE_CLIENT_ID=<client-id> frontend
```

Frontend image-i nginx-dir: statik faylları verir, `/api/` və `/ws`-i `backend:8080`-ə
ötürür (`frontend/nginx.conf`). Backend konteynerinin adı başqadırsa, bu faylı uyğunlaşdır.

### 2. Secret-lər

Bir dəfə yarat və yalnız serverin secret store-unda saxla (repoda, `.env`-də yox):

```bash
openssl rand -base64 48   # MASAPAY_JWT_SECRET
openssl rand -base64 48   # MASAPAY_PAYRIFF_WEBHOOK_SECRET
openssl rand -base64 32   # MASAPAY_SECRETS_ENCRYPTION_KEY
```

### 3. Backend-i işə salmaq

```bash
docker run -d --name backend \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/masapay \
  -e SPRING_DATASOURCE_USERNAME=masapay -e SPRING_DATASOURCE_PASSWORD=<güclü-parol> \
  -e SPRING_DATA_REDIS_HOST=<redis-host> \
  -e MASAPAY_JWT_SECRET=... -e MASAPAY_PAYRIFF_WEBHOOK_SECRET=... \
  -e MASAPAY_SECRETS_ENCRYPTION_KEY=... \
  -e MASAPAY_GOOGLE_CLIENT_ID=... -e MASAPAY_SUPER_ADMIN_EMAILS=admin@sirket.az \
  -e MASAPAY_PAYRIFF_APP_KEY=... \
  -e MASAPAY_PAYRIFF_CALLBACK_URL=https://<sayt>/api/public/payriff/webhook \
  -e MASAPAY_PAYRIFF_REDIRECT_URL=https://<sayt>/payment/result \
  -e MASAPAY_QR_BASE_URL=https://<sayt> \
  masapay-backend:<versiya>
```

`prod` profili ([`application-prod.yml`](./backend/src/main/resources/application-prod.yml))
bunları özü qoyur:

- proxy başlıqlarına etibar (`server.forward-headers-strategy=framework`), ona görə
  rate limit hər müştərinin öz IP-sini görür;
- refresh cookie yalnız HTTPS ilə (`Secure`);
- dev login bağlı, Swagger / OpenAPI bağlı (`/v3/api-docs` 404).

Flyway miqrasiyaları backend başlayanda özü işləyir.

### 4. Backend nə vaxt başlamır (bu qəsdəndir)

- `MASAPAY_JWT_SECRET` və ya `MASAPAY_PAYRIFF_WEBHOOK_SECRET` boşdur, repodakı
  default-dur və ya 32 simvoldan qısadır;
- `MASAPAY_SECRETS_ENCRYPTION_KEY` yoxdur, base64 deyil, 32 bayt deyil, və ya
  bazadakı açarları aça bilmir (səhv açar);
- refresh cookie HTTPS-siz qalıb (`MASAPAY_AUTH_COOKIE_SECURE=false`) və ya dev
  login açıqdır.

Log-dakı mesaj hansı dəyişənin səhv olduğunu deyir.

### 5. HTTPS və reverse proxy

Saytı HTTPS arxasında ver (məsələn nginx / load balancer + Let's Encrypt). Proxy
bu başlıqları ötürməlidir:

```nginx
proxy_set_header Host $host;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
# WebSocket (/ws) üçün əlavə:
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

### 6. Birdən çox backend nüsxəsi

`MASAPAY_REDIS_ENABLED=true` və bütün nüsxələr eyni Redis-ə qoşulur. Canlı
yeniləmələr Redis pub/sub ilə hər nüsxənin WebSocket müştərilərinə çatır. Rate limit
sayğacları da Redis-dədir, nüsxələr arası paylaşılır və restart-da sıfırlanmır.
Redis müvəqqəti dayansa, API işləməyə davam edir (rate limit o anda tətbiq olunmur,
log-da xəbərdarlıq yazılır).

### 7. Şifrələmə açarının rotasiyası

1. Yeni açar yarat (`openssl rand -base64 32`).
2. `MASAPAY_SECRETS_KEY_ID`-i yeni ada (məsələn `k2`), `MASAPAY_SECRETS_ENCRYPTION_KEY`-i
   yeni açara dəyiş; köhnəni `MASAPAY_SECRETS_OLD_KEYS=k1=<köhnə-açar>` kimi saxla.
3. Backend-i yenidən başlat: bazadakı bütün açarlar yeni açarla yenidən şifrələnir.
4. Növbəti buraxılışda `MASAPAY_SECRETS_OLD_KEYS`-i sil.

### 8. Yoxlama siyahısı

- [ ] `https://<sayt>/admin/login`: Google ilə giriş işləyir, `MASAPAY_SUPER_ADMIN_EMAILS`
      siyahısındakı email super admin olur.
- [ ] `https://<sayt>/v3/api-docs` 404 qaytarır.
- [ ] Müştəri QR linki açılır, canlı yeniləmələr gəlir (WebSocket `wss://`).
- [ ] Test ödənişindən sonra Payriff callback gəlir və hesab "Ödənilib" olur.
- [ ] Bazanın gündəlik backup-ı qurulub (`pg_dump`); şifrələmə açarı backup-dan
      ayrı saxlanılır.

## Layihə qaydaları

Tam qaydalar üçün [CLAUDE.md](./CLAUDE.md); backlog və issue əlaqələri üçün
[BACKLOG.md](./BACKLOG.md); dizayn üçün [docs/DESIGN.md](./docs/DESIGN.md).

- UI-da, kodda, commit-lərdə və sənədlərdə emoji qadağandır; vizual işarələr yalnız
  `lucide-react` ikonları ilə verilir.
- Əsas UI komponent kitabxanası shadcn/ui-dir.
- Qradient və bənövşəyi-mavi (#6366F1 / #8B5CF6) rənglər qadağandır.

### Git hooks (emoji qadağası)

Emoji qadağası repo kökündəki pre-commit hook ilə tətbiq olunur (husky +
`scripts/check-no-emoji.mjs`). Repo klonlandıqdan sonra kök qovluqda bir dəfə:

```bash
npm install              # husky pre-commit hook-larını quraşdırır
npm run check:no-emoji   # staged faylları əl ilə yoxla
```
