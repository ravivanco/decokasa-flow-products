import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import { defineConfig } from 'vitest/config';

// Configuración de Vite y de Vitest para el frontend de Decokasa.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    // En desarrollo, /api va al backend local; así el navegador no hace llamadas entre orígenes.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    // Las pruebas ejercitan la sesión real aunque el equipo habilite el acceso local de desarrollo.
    env: { VITE_ACCESO_LOCAL: 'false' },
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    css: true,
  },
});
