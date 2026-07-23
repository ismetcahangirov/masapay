# masapay — Frontend

React 18 + Vite + TypeScript (strict) əsaslı PWA və admin panel.

## Scripts

```bash
npm install        # asılılıqları quraşdır
npm run dev        # development server (Vite)
npm run build      # tsc type-check + production build
npm run preview    # production build-i lokal olaraq baxış
npm run lint       # ESLint
npm run format     # Prettier ilə formatla
npm run format:check
```

## Tooling

- **Vite** — dev server və build.
- **TypeScript** — `strict` mode aktivdir (`tsconfig.app.json`).
- **ESLint** (flat config, `eslint.config.js`) — `typescript-eslint`,
  `react-hooks`, `react-refresh` qaydaları; `eslint-config-prettier` ilə
  formatlama münaqişələri söndürülüb.
- **Prettier** — `.prettierrc.json`.

Növbəti epic-lərdə TailwindCSS, shadcn/ui, lucide-react, Redux Toolkit və RTK
Query əlavə olunacaq (bax `BACKLOG.md`).
