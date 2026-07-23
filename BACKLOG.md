# masapay — Backlog

GitHub Issues əsaslı backlog: **10 Epic** və **44 Sub-issue** (native parent/sub-issue
iyerarxiyası ilə). Repo: [ismetcahangirov/masapay](https://github.com/ismetcahangirov/masapay/issues).

## Assign Qaydası

- Explicit `frontend` label → **@ismetcahangirov**
- Explicit `backend` label → **@YunisPashayev**
- Hər iki label → **hər ikisi**
- Nə frontend nə backend label yoxdursa domenə görə:
  `design` / `admin-panel` → @ismetcahangirov; `database` / `payment` /
  `pos-integration` / `auth` → @YunisPashayev; təmiz `devops` → @YunisPashayev (infra).

## Epic Xülasəsi

| Epic | Issue | Sub-issue | Labels |
|---|---|---|---|
| 1. Layihə Təməli və Dizayn Sistemi | [#1](https://github.com/ismetcahangirov/masapay/issues/1) | 5 | epic, frontend, design |
| 2. Verilənlər Bazası və Backend Arxitekturası | [#7](https://github.com/ismetcahangirov/masapay/issues/7) | 5 | epic, backend, database |
| 3. Autentifikasiya və İcazələr (RBAC) | [#13](https://github.com/ismetcahangirov/masapay/issues/13) | 4 | epic, auth, backend |
| 4. Müştəri PWA — QR və Real-Time | [#18](https://github.com/ismetcahangirov/masapay/issues/18) | 4 | epic, frontend, backend |
| 5. Hesab Bölünməsi Mühərriki | [#23](https://github.com/ismetcahangirov/masapay/issues/23) | 4 | epic, frontend, backend |
| 6. Bəxşiş və Google Rəy Modulu | [#28](https://github.com/ismetcahangirov/masapay/issues/28) | 3 | epic, frontend, backend |
| 7. Payriff Ödəniş Gateway | [#32](https://github.com/ismetcahangirov/masapay/issues/32) | 5 | epic, payment, backend |
| 8. POS Sinxronizasiyası | [#38](https://github.com/ismetcahangirov/masapay/issues/38) | 3 | epic, pos-integration, backend |
| 9. Admin Panel | [#42](https://github.com/ismetcahangirov/masapay/issues/42) | 6 | epic, admin-panel, frontend |
| 10. DevOps, Test və Yerləşdirmə | [#49](https://github.com/ismetcahangirov/masapay/issues/49) | 5 | epic, devops |
| **Cəmi** | | **44** | |

## Detallı Backlog

### EPIC 1 — Layihə Təməli və Dizayn Sistemi (#1 · @ismetcahangirov)
| # | Sub-issue | Assignee | Labels |
|---|---|---|---|
| #2 | 1.1 Monorepo strukturunun qurulması | ismetcahangirov + YunisPashayev | devops, frontend, backend |
| #3 | 1.2 Frontend layihəsi (React 18 + Vite + TS) | ismetcahangirov | frontend |
| #4 | 1.3 TailwindCSS və shadcn/ui konfiqurasiyası | ismetcahangirov | frontend, design |
| #5 | 1.4 Dizayn tokenlərinin təyin edilməsi | ismetcahangirov | design, frontend |
| #6 | 1.5 İkon sistemi (lucide-react) və emoji-qadağası | ismetcahangirov | design, frontend |

### EPIC 2 — Verilənlər Bazası və Backend Arxitekturası (#7 · @YunisPashayev)
| # | Sub-issue | Assignee | Labels |
|---|---|---|---|
| #8 | 2.1 Spring Boot layihə skeleti | YunisPashayev | backend |
| #9 | 2.2 PostgreSQL 16 sxemi | YunisPashayev | database, backend |
| #10 | 2.3 JPA Entity və Repository qatı | YunisPashayev | backend, database |
| #11 | 2.4 Redis inteqrasiyası (cache + pub/sub) | YunisPashayev | backend, database |
| #12 | 2.5 Miqrasiya idarəetməsi (Flyway) | YunisPashayev | database, devops |

### EPIC 3 — Autentifikasiya və İcazələr (RBAC) (#13 · @YunisPashayev)
| # | Sub-issue | Assignee | Labels |
|---|---|---|---|
| #14 | 3.1 Google OAuth2 inteqrasiyası | YunisPashayev | auth, backend |
| #15 | 3.2 JWT token generasiyası və doğrulanması | YunisPashayev | auth, backend |
| #16 | 3.3 Rol-əsaslı icazə modeli | YunisPashayev | auth, backend |
| #17 | 3.4 Frontend auth axını | ismetcahangirov | auth, frontend |

### EPIC 4 — Müştəri PWA — QR və Real-Time (#18 · @ismetcahangirov + @YunisPashayev)
| # | Sub-issue | Assignee | Labels |
|---|---|---|---|
| #19 | 4.1 QR kod route strukturu | ismetcahangirov | frontend |
| #20 | 4.2 RTK Query ilə canlı adisyon | ismetcahangirov + YunisPashayev | frontend, backend |
| #21 | 4.3 WebSocket (STOMP) + Redis Pub/Sub | ismetcahangirov + YunisPashayev | backend, frontend |
| #22 | 4.4 Masa vəziyyəti UI komponentləri | ismetcahangirov | frontend, design |

### EPIC 5 — Hesab Bölünməsi Mühərriki (#23 · @ismetcahangirov + @YunisPashayev)
| # | Sub-issue | Assignee | Labels |
|---|---|---|---|
| #24 | 5.1 "Tam Hesabı Ödə" rejimi | ismetcahangirov + YunisPashayev | frontend, backend |
| #25 | 5.2 "Öz Yediyini Ödə" (item-based split) | ismetcahangirov + YunisPashayev | frontend, backend |
| #26 | 5.3 "Bərabər Böl" rejimi | ismetcahangirov + YunisPashayev | frontend, backend |
| #27 | 5.4 Backend SplitCalculationService | YunisPashayev | backend |

### EPIC 6 — Bəxşiş və Google Rəy Modulu (#28 · @ismetcahangirov + @YunisPashayev)
| # | Sub-issue | Assignee | Labels |
|---|---|---|---|
| #29 | 6.1 Tip seçim komponenti | ismetcahangirov | frontend, design |
| #30 | 6.2 Bəxşişin əsas məbləğə əlavə məntiqi | YunisPashayev | backend |
| #31 | 6.3 Google Review inteqrasiya modulu | ismetcahangirov | frontend |

### EPIC 7 — Payriff Ödəniş Gateway (#32 · @YunisPashayev)
| # | Sub-issue | Assignee | Labels |
|---|---|---|---|
| #33 | 7.1 PayriffService — createOrder | YunisPashayev | payment, backend |
| #34 | 7.2 HMAC imza yaradılması və doğrulanması | YunisPashayev | payment, backend |
| #35 | 7.3 Payriff Webhook listener | YunisPashayev | payment, backend |
| #36 | 7.4 Refund API inteqrasiyası | YunisPashayev | payment, backend, admin-panel |
| #37 | 7.5 Frontend ödəniş axını | ismetcahangirov | payment, frontend |

### EPIC 8 — POS Sinxronizasiyası (#38 · @YunisPashayev)
| # | Sub-issue | Assignee | Labels |
|---|---|---|---|
| #39 | 8.1 Universal POS Adapter interfeysi | YunisPashayev | pos-integration, backend |
| #40 | 8.2 iiko inteqrasiyası | YunisPashayev | pos-integration, backend |
| #41 | 8.3 Avtomatik kassa bağlanması | YunisPashayev | pos-integration, backend |

### EPIC 9 — Admin Panel (#42 · @ismetcahangirov)
| # | Sub-issue | Assignee | Labels |
|---|---|---|---|
| #43 | 9.1 Dashboard səhifəsi | ismetcahangirov | admin-panel, frontend |
| #44 | 9.2 Canlı Masalar (Live Floor Plan Monitor) | ismetcahangirov | admin-panel, frontend |
| #45 | 9.3 Əməliyyatlar və Refund Hub | ismetcahangirov | admin-panel, frontend |
| #46 | 9.4 POS və Payriff Tənzimləmələri | ismetcahangirov + YunisPashayev | admin-panel, frontend, backend |
| #47 | 9.5 QR Kod Generatoru | YunisPashayev | admin-panel, backend |
| #48 | 9.6 Bəxşişlərin Bölünməsi və Personal Analitikası | ismetcahangirov + YunisPashayev | admin-panel, frontend, backend |

### EPIC 10 — DevOps, Test və Yerləşdirmə (#49 · @ismetcahangirov + @YunisPashayev)
| # | Sub-issue | Assignee | Labels |
|---|---|---|---|
| #50 | 10.1 Docker Compose konfiqurasiyası | YunisPashayev | devops |
| #51 | 10.2 CI/CD pipeline (GitHub Actions) | YunisPashayev | devops |
| #52 | 10.3 Backend unit/integration testləri | YunisPashayev | devops, backend |
| #53 | 10.4 Frontend component testləri | ismetcahangirov | devops, frontend |
| #54 | 10.5 Production deployment sənədləşdirilməsi | YunisPashayev | devops |
