import { Outlet } from 'react-router';
import { Header } from './components/Header';
import { Sidebar } from './components/Sidebar';
import { PaisProvider, PuertaPais } from './pais/PaisProvider';

// Diseño de la aplicación con sesión (plan, tareas 19 y 21): encabezado, menú y la pantalla de la ruta activa.
// El estado vive en la sesión (SesionProvider) y en TanStack Query; las pantallas del demo de fases posteriores
// (auditoría y cronograma) siguen fuera de las rutas activas. Bodegas y avisos ya consumen la API real.
export default function App() {
  // F5-B3-H05: a 390 px el encabezado medía 456 px y recortaba "Cerrar sesión". Sin cambiar Header.tsx, el layout le
  // permite pasar el bloque de usuario a una segunda línea y encoger el nombre (que ya trunca); nada se oculta.
  // Paso 7.3: el país activo envuelve menú y contenido; el contenido solo aparece con un país autorizado elegido y se
  // vuelve a montar al cambiarlo (ADR-0024). Header.tsx no cambia.
  return (
    <PaisProvider>
    <div className="flex min-h-svh flex-col bg-background text-foreground [&>header]:flex-wrap [&>header>div:last-child]:min-w-0 [&>header>div:last-child]:max-w-full">
      <Header />
      <div className="flex flex-1 flex-col md:flex-row">
        <Sidebar />
        {/* min-w-0: sin él, un elemento flexible no se encoge por debajo del ancho de su contenido, y una tabla o la
            línea de tiempo (min-w-max) ensanchan la página; así se desplazan dentro de su propio contenedor
            (F-2.5-02; DESIGN.md 7.1 y 7.2). */}
        <main className="min-w-0 flex-1 p-4 sm:p-6">
          <PuertaPais>
            <Outlet />
          </PuertaPais>
        </main>
      </div>
    </div>
    </PaisProvider>
  );
}
