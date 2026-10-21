import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'

// The Spring Boot backend runs on :8080 in local development. Proxying /api and
// /ws keeps the browser on one origin, so no CORS setup is needed and the
// refresh-token cookie (#17) is sent like on the deployed site.
const BACKEND_URL = process.env.VITE_BACKEND_URL ?? 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    proxy: {
      '/api': { target: BACKEND_URL, changeOrigin: true },
      '/ws': { target: BACKEND_URL, changeOrigin: true, ws: true },
    },
  },
})
