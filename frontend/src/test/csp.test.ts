import { readFileSync } from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';

// M4 (revisión de seguridad del paso 1.7): el nginx del frontend envía una Content-Security-Policy. El token de acceso
// vive en memoria, así que un XSS equivale a robar la sesión; la CSP es la principal defensa. La compatibilidad real
// con la aplicación construida se comprueba además en el navegador (informe del paso 1.8).
const nginx = readFileSync(path.resolve(process.cwd(), 'nginx.conf'), 'utf-8');
const indice = readFileSync(path.resolve(process.cwd(), 'index.html'), 'utf-8');

function politica(): string {
  const linea = nginx.split('\n').find((texto) => texto.includes('Content-Security-Policy'));
  expect(linea, 'nginx.conf debe enviar Content-Security-Policy').toBeDefined();
  return linea!;
}

describe('Content-Security-Policy del frontend (M4)', () => {
  it.each([
    "default-src 'self'",
    "script-src 'self'",
    "connect-src 'self'",
    'object-src blob:',
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
  ])('incluye %s', (directiva) => {
    expect(politica()).toContain(directiva);
  });

  it('no permite eval ni scripts en línea', () => {
    expect(politica()).not.toContain("'unsafe-eval'");
    expect(politica()).not.toMatch(/script-src[^;]*'unsafe-inline'/);
  });

  it('permite el visor local sin permitir Google ni blob en scripts o conexiones', () => {
    expect(politica()).toContain("frame-src 'self' blob:");
    expect(politica()).toContain("img-src 'self' data: blob:");
    expect(politica()).not.toMatch(/(?:script|connect)-src[^;]*blob:/);
    expect(politica()).not.toContain('google.com');
  });

  it('se envía siempre (always), también en las respuestas de error', () => {
    expect(politica()).toMatch(/add_header Content-Security-Policy ".*" always;/);
  });

  it('index.html no tiene scripts en línea: solo módulos con src', () => {
    const scripts = indice.match(/<script\b[^>]*>/g) ?? [];
    expect(scripts.length).toBeGreaterThan(0);
    for (const script of scripts) {
      expect(script).toMatch(/\ssrc="/);
    }
  });
});
