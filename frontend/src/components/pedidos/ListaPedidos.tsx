import { PlusIcon, SearchIcon } from 'lucide-react';
import { type FormEvent, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { useDefinicionesVigentes, usePedidos } from '@/api/pedidos';
import { useUsuarioActual } from '@/auth/SesionProvider';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { codigoEtapa, flujosQuePuedeCrear, mensajeDeErrorPedido, nombreFlujo } from './formato';
import { Paginacion, TablaPedidos } from './TablaPedidos';

// Todos los pedidos (P-13, provisional del equipo: cualquier persona activa con un rol vigente los ve). Los filtros
// viven en la dirección para poder compartir o recargar la búsqueda; el servidor filtra y pagina.

const TAMANO = 20;

export function ListaPedidos() {
  const usuario = useUsuarioActual();
  const [parametros, setParametros] = useSearchParams();
  const texto = parametros.get('texto') ?? '';
  const flujoElegido = parametros.get('flujo');
  const tipoFlujo = flujoElegido === 'A' || flujoElegido === 'B' ? flujoElegido : undefined;
  // Fase 6: una etapa solo filtra junto con su flujo (B2 no es A2).
  const etapa = tipoFlujo ? Number(parametros.get('etapa')) || undefined : undefined;
  const vencido = parametros.get('vencido') === 'true';
  const pagina = Math.max(0, Number(parametros.get('pagina')) || 0);
  const [borrador, setBorrador] = useState(texto);

  const pedidos = usePedidos({ texto: texto || undefined, tipoFlujo, etapa, vencido, pagina, tamano: TAMANO });
  const definiciones = useDefinicionesVigentes();
  const creables = flujosQuePuedeCrear(definiciones, usuario.roles);
  const hayFiltros = Boolean(texto || tipoFlujo || vencido);
  const valorEtapa = tipoFlujo ? (etapa ? `${tipoFlujo}-${etapa}` : tipoFlujo) : 'todas';

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
        {creables.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {creables.map((tipo, indice) => (
              <Button key={tipo} variant={indice === 0 ? 'default' : 'outline'} asChild>
                <Link to={`/pedidos/nuevo?flujo=${tipo}`}>
                  <PlusIcon data-icon="inline-start" />
                  {tipo === 'B' ? 'Crear pedido propuesto' : 'Nuevo pedido'}
                </Link>
              </Button>
            ))}
          </div>
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
          <Label htmlFor="filtro-etapa">Flujo y etapa actual</Label>
          <Select
            value={valorEtapa}
            onValueChange={(valor) => {
              const [flujo, numero] = valor === 'todas' ? [] : valor.split('-');
              cambiar({ flujo, etapa: numero });
            }}
          >
            <SelectTrigger id="filtro-etapa" className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                <SelectItem value="todas">Todos los flujos y etapas</SelectItem>
              </SelectGroup>
              {(['A', 'B'] as const).map((tipo) => {
                const definicion = definiciones[tipo];
                if (!definicion) return null;
                return (
                  <SelectGroup key={tipo}>
                    <SelectLabel>{`Flujo ${nombreFlujo(tipo)}`}</SelectLabel>
                    <SelectItem value={tipo}>{`Todas las etapas de ${tipo}`}</SelectItem>
                    {definicion.etapas.map((etapaDelFlujo) => (
                      <SelectItem key={etapaDelFlujo.numero} value={`${tipo}-${etapaDelFlujo.numero}`}>
                        {codigoEtapa(tipo, etapaDelFlujo.numero)} · {etapaDelFlujo.nombre}
                      </SelectItem>
                    ))}
                  </SelectGroup>
                );
              })}
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
        recorrido={{ A: definiciones.A?.etapas, B: definiciones.B?.etapas }}
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
