import { HomeIcon, ShieldCheckIcon, UsersIcon, type LucideIcon } from 'lucide-react';
import { NavLink } from 'react-router';
import { esAdmin, useUsuarioActual } from '@/auth/SesionProvider';
import { cn } from '@/lib/utils';

// Menú principal (plan, tarea 21). Solo muestra las secciones activas de la fase 1; las pantallas del demo de fases
// posteriores (pedidos, bodegas, avisos, auditoría) quedan fuera de las rutas hasta su fase. El servidor valida igual
// cada permiso (R6).
interface Enlace {
  a: string;
  texto: string;
  icono: LucideIcon;
  soloAdmin?: boolean;
}

const ENLACES: Enlace[] = [
  { a: '/inicio', texto: 'Inicio', icono: HomeIcon },
  { a: '/usuarios', texto: 'Usuarios', icono: UsersIcon, soloAdmin: true },
  { a: '/roles', texto: 'Roles', icono: ShieldCheckIcon, soloAdmin: true },
];

export function Sidebar() {
  const usuario = useUsuarioActual();
  const visibles = ENLACES.filter((enlace) => !enlace.soloAdmin || esAdmin(usuario));

  return (
    <nav aria-label="Menú principal" className="border-b bg-card md:w-56 md:shrink-0 md:border-r md:border-b-0">
      <ul className="flex gap-1 overflow-x-auto p-2 md:flex-col">
        {visibles.map(({ a, texto, icono: Icono }) => (
          <li key={a}>
            <NavLink
              to={a}
              className={({ isActive }) =>
                cn(
                  'flex min-h-10 items-center gap-2 rounded-md px-3 text-sm font-medium outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring',
                  isActive && 'bg-secondary text-primary',
                )
              }
            >
              <Icono aria-hidden="true" className="size-4" />
              {texto}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
