import { PlusIcon, SearchIcon } from 'lucide-react';
import { type FormEvent, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { useDefinicionFlujo, usePedidos } from '@/api/pedidos';
import { esAdmin, useUsuarioActual } from '@/auth/SesionProvider';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { codigoEtapa, mensajeDeErrorPedido } from './formato';
import { Paginacion, TablaPedidos } from './TablaPedidos';

// Todos los pedidos (P-13, provisional del equipo: cualquier persona activa con un rol vigente los ve). Los filtros
// viven en la dirección para poder compartir o recargar la búsqueda; el servidor filtra y pagina.

const TAMANO = 20;

export function ListaPedidos() {
  const usuario = useUsuarioActual();
  const [parametros, setParametros] = useSearchParams();
  const texto = parametros.get('texto') ?? '';
  const etapa = Number(parametros.get('etapa')) || undefined;
  const vencido = parametros.get('vencido') === 'true';
  const pagina = Math.max(0, Number(parametros.get('pagina')) || 0);
  const [borrador, setBorrador] = useState(texto);

  const pedidos = usePedidos({ texto: texto || undefined, etapa, vencido, pagina, tamano: TAMANO });
  // En la fase 2 solo está sembrado el flujo A de Ecuador (P-14 sigue pendiente para el B).
  const flujo = useDefinicionFlujo('EC', 'A', undefined);
  const hayFiltros = Boolean(texto || etapa || vencido);

  function cambiar(cambios: Record<string, string | undefined>) {
    const siguiente = new URLSearchParams(parametros);
    for (const [clave, valor] of Object.entries(cambios)) {
      if (valor) siguiente.set(clave, valor);
      else siguiente.delete(clave);
    }
    // Un filtro nuevo vuelve a la primera página.
    if (!('pagina' in cambios)) siguiente.delete('pagina');
    setParametros(siguiente, { replace: true });
  }

  function buscar(evento: FormEvent) {
    evento.preventDefault();
    cambiar({ texto: borrador.trim() || undefined });
  }

  function limpiar() {
    setBorrador('');
    setParametros(new URLSearchParams(), { replace: true });
  }

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="font-heading text-2xl font-bold text-primary">Pedidos</h1>
        {esAdmin(usuario) && (
          <Button asChild>
            <Link to="/pedidos/nuevo">
              <PlusIcon data-icon="inline-start" />
              Nuevo pedido
            </Link>
          </Button>
        )}
      </div>

      <div className="flex flex-col gap-4 rounded-lg border bg-card p-4 shadow-sm lg:flex-row lg:items-end">
        <form role="search" onSubmit={buscar} className="flex flex-1 flex-col gap-2">
          <Label htmlFor="buscar-pedido">Buscar por número o nombre</Label>
          <div className="flex gap-2">
            <Input
              id="buscar-pedido"
              type="search"
              value={borrador}
              onChange={(evento) => setBorrador(evento.target.value)}
              placeholder="DK-EC-2026-0001"
            />
            <Button type="submit" variant="outline">
              <SearchIcon data-icon="inline-start" />
              Buscar
            </Button>
          </div>
        </form>
        <div className="flex flex-col gap-2 lg:w-72">
          <Label htmlFor="filtro-etapa">Etapa actual</Label>
          <Select
            value={etapa ? String(etapa) : 'todas'}
            onValueChange={(valor) => cambiar({ etapa: valor === 'todas' ? undefined : valor })}
          >
            <SelectTrigger id="filtro-etapa" className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                <SelectItem value="todas">Todas las etapas</SelectItem>
                {flujo.data?.etapas.map((definicion) => (
                  <SelectItem key={definicion.numero} value={String(definicion.numero)}>
                    {codigoEtapa('A', definicion.numero)} · {definicion.nombre}
                  </SelectItem>
                ))}
              </SelectGroup>
            </SelectContent>
          </Select>
        </div>
        <div className="flex min-h-10 items-center gap-2">
          <Checkbox
            id="filtro-vencido"
            checked={vencido}
            onCheckedChange={(marcado) => cambiar({ vencido: marcado === true ? 'true' : undefined })}
          />
          <Label htmlFor="filtro-vencido">Solo vencidos</Label>
        </div>
      </div>

      <TablaPedidos
        titulo="Pedidos, del más reciente al más antiguo"
        pedidos={pedidos.data?.elementos}
        cargando={pedidos.isPending}
        error={pedidos.isError ? mensajeDeErrorPedido(pedidos.error, 'No se pudieron cargar los pedidos.') : null}
        onReintentar={() => void pedidos.refetch()}
        vacio={
          hayFiltros ? (
            <div className="flex flex-col items-center gap-3">
              <p>Ningún pedido coincide con los filtros.</p>
              <Button variant="outline" onClick={limpiar}>
                Limpiar filtros
              </Button>
            </div>
          ) : (
            <p className="text-muted-foreground">Todavía no hay pedidos.</p>
          )
        }
      />

      {pedidos.data && (
        <Paginacion
          pagina={pedidos.data.pagina}
          tamano={pedidos.data.tamano}
          total={pedidos.data.total}
          ocupado={pedidos.isFetching}
          onCambiar={(nueva) => cambiar({ pagina: nueva > 0 ? String(nueva) : undefined })}
        />
      )}
    </section>
  );
}
