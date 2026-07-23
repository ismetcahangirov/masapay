# masapay — Frontend

React 18 + Vite + TypeScript (strict) əsaslı PWA və admin panel.

## Scripts

```bash
npm install        # asılılıqları quraşdır
npm run dev        # development server (Vite)
npm run build      # tsc type-check + production build
npm run preview    # production build-i lokal olaraq baxış
npm run lint       # ESLint
npm run lint:css   # Stylelint (dizayn tokenləri + qadağan qaydaları)
npm run format     # Prettier ilə formatla
npm run format:check
```

## Tooling

- **Vite** — dev server və build.
- **TypeScript** — `strict` mode aktivdir (`tsconfig.app.json`).
- **TailwindCSS + shadcn/ui** — dizayn sistemi (`tailwind.config.js`,
  `components.json`, `src/components/ui`).
- **ESLint** (flat config, `eslint.config.js`) — `typescript-eslint`,
  `react-hooks`, `react-refresh` qaydaları; `eslint-config-prettier` ilə
  formatlama münaqişələri söndürülüb.
- **Stylelint** (`.stylelintrc.json`) — dizayn tokeni qaydaları.
- **Prettier** — `.prettierrc.json`.

## Dizayn Tokenləri

Rəng palitrası `src/index.css` içində HSL CSS dəyişənləri kimi təyin olunub və
`tailwind.config.js` bunları semantik siniflərə (`bg-background`, `text-primary`,
`bg-success` və s.) bağlayır. Komponentlərdə birbaşa hex rəng yazmaq əvəzinə
həmişə bu tokenlərdən istifadə et.

| Token | Dəyər | Təyinat |
|---|---|---|
| `--background` | `#FAFAFA` | Səhifə fonu |
| `--card` / `--popover` | `#FFFFFF` | Səth |
| `--foreground` / `--primary` | `#0F172A` | Mətn / düymə |
| `--success` | `#10B981` | Uğur / CTA |
| `--border` / `--input` | `#E2E8F0` | Sərhəd |

## Lint Qaydaları

`npm run lint:css` (Stylelint) qradientləri (`function-disallowed-list`) və
qadağan olunmuş bənövşəyi-mavi rəngləri (`#6366F1`, `#8B5CF6`) bloklayır.

## İkon Sistemi

Bütün vizual işarələr (status, aksiya, naviqasiya) üçün YALNIZ
[`lucide-react`](https://lucide.dev) ikon komponentlərindən istifadə olunur.
Emoji istifadəsi qadağandır və repo kökündəki pre-commit hook ilə yoxlanılır
(bax kök `scripts/check-no-emoji.mjs`).

Növbəti epic-lərdə Redux Toolkit və RTK Query əlavə olunacaq (bax `BACKLOG.md`).
