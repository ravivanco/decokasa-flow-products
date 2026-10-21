import { InboxIcon, PlusIcon } from 'lucide-react';
import { Link } from 'react-router';
import { useDefinicionesVigentes } from '@/api/pedidos';
import { esAdmin, useUsuarioActual } from '@/auth/SesionProvider';
import { flujosQuePuedeCrear } from '@/components/pedidos/formato';
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
import { BuscarPorNumero } from './BuscarPorNumero';
import { PanelVencidos } from './PanelVencidos';
import { ProductosPropuestos } from './ProductosPropuestos';
import { TableroTrabajo } from './TableroTrabajo';

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

// Paso 5.3: el dashboard responde, con datos reales, qué toca hacer (mi trabajo), qué va tarde y por qué (vencidos del
// admin) y dónde está un pedido (búsqueda por número). Quien solo tiene paneles provisionales (consulta,
// asistente_china) ve la búsqueda y sus paneles, sin bandeja de trabajo.
export function DashboardRol() {
  const usuario = useUsuarioActual();
  const admin = esAdmin(usuario);
  const paneles = panelesDe(usuario.roles);
  const conTrabajo = admin || paneles.some((panel) => !panel.provisional);
  // Fase 6: crear según las definiciones (A el admin, B Compras China; P-14). El servidor lo vuelve a decidir.
  const creables = flujosQuePuedeCrear(useDefinicionesVigentes(), usuario.roles);

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-heading text-2xl font-bold text-primary">Hola, {usuario.nombre}</h1>
        {creables.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {creables.map((tipo) => (
              <Button key={tipo} variant="outline" asChild>
                <Link to={`/pedidos/nuevo?flujo=${tipo}`}>
                  <PlusIcon data-icon="inline-start" />
                  {tipo === 'B' ? 'Crear pedido propuesto' : 'Nuevo pedido'}
                </Link>
              </Button>
            ))}
          </div>
        )}
      </div>
      <BuscarPorNumero />
      {conTrabajo && <TableroTrabajo admin={admin} />}
      <ProductosPropuestos />
      {admin && <PanelVencidos />}
      <div className="grid gap-4 lg:grid-cols-2">
        {paneles.map((panel) => (
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
