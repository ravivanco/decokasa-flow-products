import { readdirSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';

// DESIGN.md 3.3 y 8 (auditoría Q-foco): el anillo de foco usa el color --ring completo (#B23F00, 5.83:1).
// Con transparencia (por ejemplo ring-ring/50) el contraste baja y el foco deja de ser claramente visible.
const carpeta = path.resolve(process.cwd(), 'src/components/ui');
const componentes = readdirSync(carpeta).filter((nombre) => nombre.endsWith('.tsx') && !nombre.includes('.test.'));

describe('componentes base de shadcn/ui', () => {
  it.each(componentes)('%s no usa el anillo de foco con transparencia', (nombre) => {
    const fuente = readFileSync(path.join(carpeta, nombre), 'utf-8');
    expect(fuente).not.toMatch(/ring-ring\/\d+/);
  });

  it.each(componentes)('%s no usa textos menores de 12 px', (nombre) => {
    const fuente = readFileSync(path.join(carpeta, nombre), 'utf-8');
    expect(fuente).not.toMatch(/text-\[(\d+px|0\.[0-6]\d*rem)\]/);
  });
});
