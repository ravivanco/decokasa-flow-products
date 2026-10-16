import { Outlet } from 'react-router';
import { Header } from './components/Header';
import { Sidebar } from './components/Sidebar';

// Diseño de la aplicación con sesión (plan, tareas 19 y 21): encabezado, menú y la pantalla de la ruta activa.
// El estado vive en la sesión (SesionProvider) y en TanStack Query; las pantallas del demo de fases posteriores
// (bodegas, avisos, auditoría, cronograma) siguen en src/components, fuera de las rutas activas.
export default function App() {
  return (
    <div className="flex min-h-svh flex-col bg-background text-foreground">
      <Header />
      <div className="flex flex-1 flex-col md:flex-row">
        <Sidebar />
        {/* min-w-0: sin él, un elemento flexible no se encoge por debajo del ancho de su contenido, y una tabla o la
            línea de tiempo (min-w-max) ensanchan la página; así se desplazan dentro de su propio contenedor
            (F-2.5-02; DESIGN.md 7.1 y 7.2). */}
        <main className="min-w-0 flex-1 p-4 sm:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
