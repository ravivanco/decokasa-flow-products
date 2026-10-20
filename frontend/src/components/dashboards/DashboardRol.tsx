import { InboxIcon } from 'lucide-react';
import { Link } from 'react-router';
import { useUsuarioActual } from '@/auth/SesionProvider';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';

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
  asistente_compras: { titulo: 'Revisión', descripcion: 'Pedidos pendientes de revisión en Ecuador.' },
  marketing: { titulo: 'Codificación', descripcion: 'Pedidos pendientes de codificación y fotos.' },
  compras_china: { titulo: 'Compras China', descripcion: 'Análisis, fabricación, embarque y aduana.' },
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

export function DashboardRol() {
  const usuario = useUsuarioActual();

  return (
    <section className="flex flex-col gap-6">
      <h1 className="font-heading text-2xl font-bold text-primary">Hola, {usuario.nombre}</h1>
      <div className="grid gap-4 lg:grid-cols-2">
        {panelesDe(usuario.roles).map((panel) => (
          <Card key={panel.titulo}>
            <CardHeader>
              <CardTitle>
                <h2 className="font-heading text-lg font-semibold">{panel.titulo}</h2>
              </CardTitle>
              <CardDescription>{panel.descripcion}</CardDescription>
              {panel.provisional && (
                <CardAction>
                  <Badge variant="outline">Provisional</Badge>
                </CardAction>
              )}
            </CardHeader>
            <CardContent className="flex flex-wrap items-center justify-between gap-3">
              <p className="flex items-center gap-3 text-muted-foreground">
                <InboxIcon aria-hidden="true" className="size-5 shrink-0" />
                {panel.provisional ? 'Consulta el estado de los pedidos.' : 'Tus pedidos pendientes están en la bandeja.'}
              </p>
              <Button variant="outline" asChild>
                <Link to={panel.provisional ? '/pedidos' : '/bandeja'}>
                  {panel.provisional ? 'Ver pedidos' : 'Abrir mi bandeja'}
                </Link>
              </Button>
            </CardContent>
            {panel.provisional && (
              <CardFooter>
                <p className="text-muted-foreground">{panel.provisional}</p>
              </CardFooter>
            )}
          </Card>
        ))}
      </div>
    </section>
  );
}
