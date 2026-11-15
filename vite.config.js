import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const API_SERVER = 'http://localhost:3001'   // old Express admin
const SPRING = process.env.BACKEND_URL || 'http://localhost:8080'   // new Spring Boot backend

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // new backend first, then the old Express admin
      '/api/auth': SPRING, '/api/public': SPRING, '/api/orders': SPRING, '/api/business': SPRING,
      '/api/catalog': SPRING, '/api/scan-points': SPRING, '/api/owner': SPRING,
      '/ws': { target: SPRING, ws: true },
      '/api': API_SERVER,
      '/r': API_SERVER,
    },
  },
})
