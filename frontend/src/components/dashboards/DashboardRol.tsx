import { InboxIcon, ListIcon, PlusIcon } from 'lucide-react';
import { Link } from 'react-router';
import { useDefinicionesVigentes } from '@/api/pedidos';
import { useUsuarioActual } from '@/auth/SesionProvider';
import { flujosQuePuedeCrear } from '@/components/pedidos/formato';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { BuscarPorNumero } from './BuscarPorNumero';
import { EstadisticasInicio } from './EstadisticasInicio';

// Dashboard vacío por rol (plan, tarea 24; D7; manual, fase 1, paso 1.5). Un usuario con varios roles ve un panel por
// cada uno, sin repetir. No hay datos simulados: cada panel lleva a la bandeja real (fase 2, paso 2.5).
//
// Alcance provisional (⏸ pendiente del cliente, requerimientos.md §5, "Puntos por confirmar"):
//  - asistente_china (Andrea Collaguazo): "Asistente de compras en China nada más". La propuesta es solo consultar
//    pedidos, sin acciones.
//  - consulta: lo conservó el equipo (2026-10-12). La propuesta es un dashboard de consulta sin permisos de gestión.
// Ninguno de los dos recibe permisos ni acciones aquí: el panel solo informa. Un rol que no tiene panel propio (por
// ejemplo, uno creado por el admin) también ve el de consulta, sin permisos adicionales.

interface Panel {
  titulo: string;
  descripcion: string;
  /** Alcance sin confirmar por el cliente (⏸): se muestra como tal. */
  provisional?: string;
}

const CONSULTA: Panel = {
  titulo: 'Consulta',
  descripcion: 'Vista de solo lectura del estado de los pedidos.',
  provisional:
    'Alcance provisional, pendiente de confirmar con el cliente: solo consulta de pedidos, sin acciones ni permisos de gestión.',
};

const PANELES: Record<string, Panel> = {
  admin: { titulo: 'Administración', descripcion: 'Usuarios, roles y visión general de los pedidos.' },
  asistente_compras: {
    titulo: 'Revisión',
    descripcion: 'Productos propuestos por China y pedidos pendientes de revisión en Ecuador.',
  },
  marketing: { titulo: 'Codificación', descripcion: 'Pedidos pendientes de codificación y fotos.' },
  compras_china: {
    titulo: 'Compras China',
    descripcion: 'Pedidos propuestos, análisis, fabricación, embarque y aduana.',
  },
  logistica: { titulo: 'Logística', descripcion: 'Autorización de salida, bodega e incidencias.' },
  asistente_china: CONSULTA,
  consulta: CONSULTA,
};

export function panelesDe(roles: string[]): Panel[] {
  const vistos = new Set<string>();
  return roles
    .map((rol) => PANELES[rol] ?? CONSULTA)
    .filter((panel) => (vistos.has(panel.titulo) ? false : (vistos.add(panel.titulo), true)));
}

// ADR-0029 (aclaración aprobada el 2026-10-26): el Inicio es el mismo dashboard estadístico para todos los roles; los
// permisos del servidor deciden los datos. «Mi trabajo», los productos propuestos, los vencidos del admin y las
// alertas viven en Mi bandeja; aquí quedan una cabecera compacta (saludo, roles, búsqueda y accesos) y las
// estadísticas como contenido principal.
export function DashboardRol() {
  const usuario = useUsuarioActual();
  const paneles = panelesDe(usuario.roles);
  // Fase 6: crear según las definiciones (A el admin, B Compras China; P-14). El servidor lo vuelve a decidir.
  const creables = flujosQuePuedeCrear(useDefinicionesVigentes(), usuario.roles);

  return (
    <section className="flex flex-col gap-4">
      <EstadisticasInicio
        encabezado={(
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="flex min-w-0 flex-col gap-1">
              <h1 className="font-heading text-2xl font-bold text-primary">Hola, {usuario.nombre}</h1>
              <ul className="flex flex-wrap items-center gap-1.5" aria-label="Mis roles">
                {paneles.map((panel) => (
                  <li key={panel.titulo}>
                    <Badge variant="outline" title={panel.provisional ?? panel.descripcion}>
                      {panel.titulo}{panel.provisional ? ' · provisional' : ''}
                    </Badge>
                  </li>
                ))}
              </ul>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <BuscarPorNumero compacta />
              <Button variant="outline" size="sm" asChild>
                <Link to="/bandeja"><InboxIcon data-icon="inline-start" />Mi bandeja</Link>
              </Button>
              <Button variant="outline" size="sm" asChild>
                <Link to="/pedidos"><ListIcon data-icon="inline-start" />Pedidos</Link>
              </Button>
              {creables.map((tipo) => (
                <Button key={tipo} size="sm" asChild>
                  <Link to={`/pedidos/nuevo?flujo=${tipo}`}>
                    <PlusIcon data-icon="inline-start" />
                    {tipo === 'B' ? 'Crear pedido propuesto' : 'Nuevo pedido'}
                  </Link>
                </Button>
              ))}
            </div>
          </div>
        )}
      />
    </section>
  );
}
