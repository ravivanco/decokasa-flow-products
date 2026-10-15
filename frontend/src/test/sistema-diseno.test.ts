import { readFileSync } from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';

// Comprueba que el frontend aplica los tokens de documentos/diseno/DESIGN.md (sección 6)
// y que las fuentes se sirven desde el propio servidor, sin Google Fonts (David trabaja desde China).
const leer = (ruta: string) => readFileSync(path.resolve(process.cwd(), ruta), 'utf-8');

describe('sistema de diseño', () => {
  const css = leer('src/index.css');

  it('define los colores de marca y de estado de DESIGN.md', () => {
    expect(css).toMatch(/--color-marca-morado:\s*#31275B/i);
    expect(css).toMatch(/--color-marca-naranja:\s*#FE5E00/i);
    expect(css).toMatch(/--color-marca-naranja-texto:\s*#B23F00/i);
    expect(css).toMatch(/--color-estado-ok:\s*#15803D/i);
    expect(css).toMatch(/--color-estado-aviso:\s*#B45309/i);
    expect(css).toMatch(/--color-estado-vencido:\s*#B91C1C/i);
    expect(css).toMatch(/--color-estado-neutro:\s*#475569/i);
  });

  it('define las variables de shadcn/ui con el morado como primario y el foco accesible', () => {
    expect(css).toMatch(/--primary:\s*#31275B/i);
    expect(css).toMatch(/--primary-foreground:\s*#FFFFFF/i);
    expect(css).toMatch(/--ring:\s*#B23F00/i);
    expect(css).toMatch(/--destructive:\s*#B91C1C/i);
  });

  it('no conserva el amarillo del demo como token de marca', () => {
    expect(css).not.toMatch(/--color-brand-yellow/i);
  });

  it('usa Inter y Montserrat servidas localmente, sin Google Fonts', () => {
    expect(css).toMatch(/--font-sans:\s*"Inter"/);
    expect(css).toMatch(/--font-titulo:\s*"Montserrat"/);
    expect(leer('src/main.tsx')).toMatch(/@fontsource(-variable)?\/inter/);
    expect(leer('src/main.tsx')).toMatch(/@fontsource(-variable)?\/montserrat/);
    expect(leer('index.html')).not.toMatch(/fonts\.googleapis\.com|fonts\.gstatic\.com/);
    expect(css).not.toMatch(/fonts\.googleapis\.com|fonts\.gstatic\.com/);
  });

  it('respeta la preferencia de movimiento reducido', () => {
    expect(css).toMatch(/prefers-reduced-motion:\s*reduce/);
  });
});
