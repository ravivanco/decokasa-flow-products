import { Navigate, Outlet, useLocation } from 'react-router';
import { SinPermiso } from '@/components/PaginasSimples';
import { esAdmin, useSesion } from './SesionProvider';

// Protección de rutas en la pantalla (plan, tarea 19). La protección real está en el servidor (R6): esto solo evita
// mostrar lo que el usuario no puede usar.

/** Aviso mientras se recupera la sesión con la cookie (D3b). */
export function Cargando() {
  return (
    <div role="status" className="flex min-h-svh items-center justify-center text-muted-foreground">
      Cargando…
    </div>
  );
}

/** Sin sesión lleva al login y recuerda la página pedida para volver después. */
export function RutaProtegida() {
  const { estado } = useSesion();
  const ubicacion = useLocation();
  if (estado.fase === 'cargando') return <Cargando />;
  if (estado.fase === 'anonima') {
    return <Navigate to="/login" replace state={{ desde: `${ubicacion.pathname}${ubicacion.search}` }} />;
  }
  return <Outlet />;
}

/** Solo para el rol admin; con otros roles muestra "Sin permiso" en la misma dirección. */
export function SoloAdmin() {
  const { estado } = useSesion();
  if (estado.fase !== 'abierta' || !esAdmin(estado.usuario)) return <SinPermiso />;
  return <Outlet />;
}
