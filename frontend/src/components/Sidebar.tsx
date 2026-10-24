import { HomeIcon, InboxIcon, MailIcon, PackageIcon, ShieldCheckIcon, TargetIcon, UsersIcon, WarehouseIcon, type LucideIcon } from 'lucide-react';
import { NavLink } from 'react-router';
import { esAdmin, useUsuarioActual } from '@/auth/SesionProvider';
import { cn } from '@/lib/utils';
import { SelectorPais } from '@/pais/PaisProvider';

// Menú principal (plan, tarea 21). Muestra las secciones activas: inicio, bandeja y pedidos (fase 2) para todos, y
// usuarios, roles y correos para el admin; bodegas para admin o Logística. El servidor valida cada permiso (R6).
interface Enlace {
  a: string;
  texto: string;
  icono: LucideIcon;
  soloAdmin?: boolean;
  /** Oculto si la persona solo tiene roles de consulta (P-13; plan, tarea 17). */
  conBandeja?: boolean;
  soloBodegas?: boolean;
}

/** Roles de solo consulta (alcance provisional, ⏸): no conceden bandeja, pero no quitan la que dan otros roles. */
const ROLES_CONSULTA = new Set(['consulta', 'asistente_china']);

const ENLACES: Enlace[] = [
  { a: '/inicio', texto: 'Inicio', icono: HomeIcon },
  { a: '/bandeja', texto: 'Mi bandeja', icono: InboxIcon, conBandeja: true },
  { a: '/pedidos', texto: 'Pedidos', icono: PackageIcon },
  { a: '/bodegas', texto: 'Bodegas', icono: WarehouseIcon, soloBodegas: true },
  { a: '/notificaciones', texto: 'Correos', icono: MailIcon, soloAdmin: true },
  { a: '/usuarios', texto: 'Usuarios', icono: UsersIcon, soloAdmin: true },
  { a: '/roles', texto: 'Roles', icono: ShieldCheckIcon, soloAdmin: true },
  { a: '/configuracion/plazos', texto: 'Meta de plazos', icono: TargetIcon, soloAdmin: true },
];

export function Sidebar() {
  const usuario = useUsuarioActual();
  const tieneBandeja = usuario.roles.some((rol) => !ROLES_CONSULTA.has(rol));
  const visibles = ENLACES.filter(
    (enlace) => (!enlace.soloAdmin || esAdmin(usuario)) && (!enlace.conBandeja || tieneBandeja)
      && (!enlace.soloBodegas || usuario.roles.some((rol) => rol === 'admin' || rol === 'logistica')),
  );

  return (
    <nav aria-label="Menú principal" className="border-b bg-card md:w-56 md:shrink-0 md:border-r md:border-b-0">
      {/* F5-B3-H05: en móvil el menú era una tira con desplazamiento horizontal, y al recibir foco un enlace visible a
          medias ("Bodegas", "Roles") no se desplazaba y quedaba recortado contra el borde. Ahora los enlaces pasan a la
          línea siguiente: todos se ven completos, con su anillo de foco, sin desplazamiento ni texto acortado. */}
      {/* Paso 7.3: país activo, solo entre los asignados hoy (con uno solo, sin opciones). */}
      <div className="border-b p-2">
        <SelectorPais />
      </div>
      <ul className="flex flex-wrap gap-1 p-2 md:flex-col md:flex-nowrap">
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
