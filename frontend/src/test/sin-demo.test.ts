import { readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';

// Auditoría S1 y S2 (plan, tareas 21 y 23): el acceso real no conserva la contraseña de demostración, los ingresos
// rápidos ni el cambio de usuario sin autenticación. Se revisa todo el código de src/ salvo las pruebas.
const raiz = path.resolve(process.cwd(), 'src');

function archivosDeCodigo(carpeta: string): string[] {
  return readdirSync(carpeta).flatMap((nombre) => {
    const ruta = path.join(carpeta, nombre);
    if (statSync(ruta).isDirectory()) return archivosDeCodigo(ruta);
    return /\.(ts|tsx)$/.test(nombre) && !/\.test\.(ts|tsx)$/.test(nombre) && !ruta.includes(`${path.sep}test${path.sep}`)
      ? [ruta]
      : [];
  });
}

describe('Sin accesos de demostración en el código (S1, S2)', () => {
  const codigo = archivosDeCodigo(raiz).map((ruta) => ({ ruta, fuente: readFileSync(ruta, 'utf-8') }));

  it.each(['Demo2026', 'DEMO_PASSWORD', 'USUARIOS_DEMO', 'onSwitchUser', 'Simulador de Roles'])(
    'ningún archivo contiene "%s"',
    (prohibido) => {
      const con = codigo.filter(({ fuente }) => fuente.includes(prohibido)).map(({ ruta }) => path.relative(raiz, ruta));
      expect(con).toEqual([]);
    },
  );

  it('ningún archivo guarda datos en localStorage ni en sessionStorage', () => {
    const con = codigo
      .filter(({ fuente }) => /(localStorage|sessionStorage)\.setItem/.test(fuente))
      .map(({ ruta }) => path.relative(raiz, ruta));
    expect(con).toEqual([]);
  });
});
