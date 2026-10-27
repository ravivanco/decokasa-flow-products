import { useQuery, useQueryClient, type Query } from '@tanstack/react-query';
import { GlobeIcon, TriangleAlertIcon } from 'lucide-react';
import {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from 'react';
import { z } from 'zod';
import { esErrorApi } from '@/api/cliente';
import { useSesion, useUsuarioActual } from '@/auth/SesionProvider';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

// País activo (fase 7, paso 7.3; ADR-0024). La pantalla solo muestra y pide datos del país elegido; el servidor vuelve
// a comprobar en cada petición que esté asignado hoy (403/404 si no). Reglas:
//  - Solo se ofrecen los países asignados hoy. Se releen con GET /auth/yo cada 30 s (mismo intervalo que ADR-0014) y al
//    volver a la pestaña; si el país elegido se revoca, sus datos se cancelan y se borran de la caché, y se exige otra
//    selección autorizada.
//  - Con un solo país, queda elegido sin ofrecer otros. Sin ninguno, no se muestra ningún dato.
//  - Cada clave de caché con datos de un país lleva el segmento `pais:XX` (segmentoPais). Al cambiar de país se
//    cancelan y se borran las consultas del anterior: una respuesta tardía no llega a ninguna pantalla.
//  - Cambiar de país con cambios sin guardar pide confirmación; nunca se trasladan a otro país. Al confirmar, las
//    pantallas se vuelven a montar (key por país) y lo sin guardar se descarta.

export const PAISES = ['EC', 'CO'] as const;
export type CodigoPais = (typeof PAISES)[number];

export const NOMBRE_PAIS: Record<CodigoPais, string> = { EC: 'Ecuador', CO: 'Colombia' };

/** Segmento de clave de caché de los datos de un país. */
export const segmentoPais = (pais: CodigoPais) => `pais:${pais}` as const;

export const esPais = (valor: string): valor is CodigoPais => (PAISES as readonly string[]).includes(valor);

/** ¿La consulta guarda datos de ese país? */
const esDelPais = (pais: CodigoPais) => (consulta: Query) => consulta.queryKey.includes(segmentoPais(pais));

const esquemaPaisesVigentes = z.object({ paises: z.array(z.string()) });

interface ValorPais {
  /** Nulo mientras no hay una selección autorizada. */
  pais: CodigoPais | null;
  asignados: CodigoPais[];
  /** Pide cambiar de país; si hay cambios sin guardar, primero pide confirmación. */
  solicitarCambio: (pais: CodigoPais) => void;
  registrarSinGuardar: (id: string, descripcion: string | null) => void;
}

const ContextoPais = createContext<ValorPais | null>(null);

const claveGuardada = (usuarioId: string) => `decokasa.pais.${usuarioId}`;

function leerGuardado(usuarioId: string): string | null {
  try {
    return window.sessionStorage.getItem(claveGuardada(usuarioId));
  } catch {
    return null;
  }
}

function guardar(usuarioId: string, pais: CodigoPais | null) {
  try {
    if (pais) window.sessionStorage.setItem(claveGuardada(usuarioId), pais);
    else window.sessionStorage.removeItem(claveGuardada(usuarioId));
  } catch {
    // Solo es una comodidad: sin almacenamiento, se vuelve a elegir.
  }
}

export function PaisProvider({ children }: { children: ReactNode }) {
  const usuario = useUsuarioActual();
  const { pedir, sesionId } = useSesion();
  const consultas = useQueryClient();

  // Países vigentes en la base (no los del token): /auth/yo los relee.
  const vigentes = useQuery({
    queryKey: ['sesion', sesionId, 'paises-vigentes'],
    queryFn: ({ signal }) => pedir('/auth/yo', { esquema: esquemaPaisesVigentes, signal }),
    refetchInterval: 30_000,
    refetchOnWindowFocus: true,
    staleTime: 10_000,
  });
  // Un 403 o un 404 del servidor puede ser un país recién revocado: se releen los países en el acto, sin esperar al
  // intervalo, para dejar de mostrar los datos del país revocado cuanto antes.
  useEffect(() => {
    return consultas.getQueryCache().subscribe((evento) => {
      if (evento.type !== 'updated' || evento.action.type !== 'error') return;
      if (evento.query.queryKey[2] === 'paises-vigentes') return;
      const error = evento.action.error;
      if (esErrorApi(error) && (error.status === 403 || error.status === 404)) {
        void consultas.invalidateQueries({ queryKey: ['sesion', sesionId, 'paises-vigentes'] });
      }
    });
  }, [consultas, sesionId]);
  const fuente = vigentes.data?.paises ?? usuario.paises;
  const firma = fuente.join(',');
  // `firma` resume el contenido de `fuente`: el arreglo solo cambia de identidad si cambian los países.
  const asignados = useMemo(() => firma.split(',').filter(esPais), [firma]);

  const [pais, setPais] = useState<CodigoPais | null>(() => {
    const guardado = leerGuardado(usuario.id);
    if (guardado && esPais(guardado) && asignados.includes(guardado)) return guardado;
    return asignados.length === 1 ? asignados[0] : null;
  });
  const [aviso, setAviso] = useState<string | null>(null);
  const [pendiente, setPendiente] = useState<CodigoPais | null>(null);
  const sinGuardar = useRef(new Map<string, string>());
  const actual = useRef(pais);
  actual.current = pais;

  const aplicar = useCallback(
    (nuevo: CodigoPais | null) => {
      const anterior = actual.current;
      if (anterior && anterior !== nuevo) {
        // Primero se cancela y se borra lo del país anterior: ninguna respuesta tardía llega a la pantalla.
        void consultas.cancelQueries({ predicate: esDelPais(anterior) });
        consultas.removeQueries({ predicate: esDelPais(anterior) });
      }
      sinGuardar.current.clear();
      actual.current = nuevo;
      setPais(nuevo);
      guardar(usuario.id, nuevo);
    },
    [consultas, usuario.id],
  );

  // Revocación o asignación: el país elegido debe seguir asignado; con uno solo, queda elegido.
  useEffect(() => {
    if (pais && !asignados.includes(pais)) {
      setAviso(`Ya no tienes acceso a ${NOMBRE_PAIS[pais]}. Sus datos dejaron de mostrarse.`);
      aplicar(asignados.length === 1 ? asignados[0] : null);
    } else if (!pais && asignados.length === 1) {
      aplicar(asignados[0]);
    }
  }, [asignados, pais, aplicar]);

  const solicitarCambio = useCallback(
    (nuevo: CodigoPais) => {
      if (nuevo === actual.current || !asignados.includes(nuevo)) return;
      if (sinGuardar.current.size > 0) setPendiente(nuevo);
      else {
        setAviso(null);
        aplicar(nuevo);
      }
    },
    [asignados, aplicar],
  );

  const registrarSinGuardar = useCallback((id: string, descripcion: string | null) => {
    if (descripcion) sinGuardar.current.set(id, descripcion);
    else sinGuardar.current.delete(id);
  }, []);

  const valor = useMemo(
    () => ({ pais, asignados, solicitarCambio, registrarSinGuardar }),
    [pais, asignados, solicitarCambio, registrarSinGuardar],
  );
  const nombreActual = pais ? NOMBRE_PAIS[pais] : 'este país';

  return (
    <ContextoPais.Provider value={valor}>
      {aviso && (
        <div className="px-4 pt-4 sm:px-6">
          <Alert>
            <TriangleAlertIcon />
            <AlertTitle>Cambio de países asignados</AlertTitle>
            <AlertDescription className="flex flex-col items-start gap-3">
              <p>{aviso}</p>
              <Button variant="outline" size="sm" onClick={() => setAviso(null)}>Entendido</Button>
            </AlertDescription>
          </Alert>
        </div>
      )}
      {children}
      <Dialog open={pendiente !== null} onOpenChange={(abierto) => !abierto && setPendiente(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>¿Cambiar a {pendiente ? NOMBRE_PAIS[pendiente] : ''}?</DialogTitle>
            <DialogDescription>
              Tienes cambios sin guardar en {nombreActual}: {[...sinGuardar.current.values()].join('; ')}. No se
              trasladan a otro país: si cambias, se descartan.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPendiente(null)}>Seguir en {nombreActual}</Button>
            <Button
              variant="destructive"
              onClick={() => {
                const destino = pendiente;
                setPendiente(null);
                if (destino && asignados.includes(destino)) {
                  setAviso(null);
                  aplicar(destino);
                }
              }}
            >
              Descartar y cambiar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </ContextoPais.Provider>
  );
}

/**
 * País activo para las consultas. Fuera de {@link PaisProvider} (pruebas aisladas de un componente) responde Ecuador:
 * es solo compatibilidad documentada; el servidor sigue autorizando cada petición con los países vigentes.
 */
export function usePaisActivo(): CodigoPais {
  return useContext(ContextoPais)?.pais ?? 'EC';
}

/** Selección completa, para el selector y los avisos de "otro país". Nula fuera del proveedor. */
export function useSeleccionPais() {
  return useContext(ContextoPais);
}

/**
 * Marca cambios sin guardar mientras {@code descripcion} no sea nula: cambiar de país pedirá confirmación antes de
 * descartarlos.
 */
export function useCambiosSinGuardar(descripcion: string | null) {
  const contexto = useContext(ContextoPais);
  const id = useId();
  const registrar = contexto?.registrarSinGuardar;
  useEffect(() => {
    registrar?.(id, descripcion);
    return () => registrar?.(id, null);
  }, [registrar, id, descripcion]);
}

/** Muestra el contenido solo con un país autorizado elegido; cada cambio de país lo vuelve a montar. */
export function PuertaPais({ children }: { children: ReactNode }) {
  const contexto = useContext(ContextoPais);
  if (!contexto) return children;
  const { pais, asignados, solicitarCambio } = contexto;
  if (asignados.length === 0) {
    return (
      <Alert className="max-w-2xl">
        <GlobeIcon />
        <AlertTitle>Sin países asignados</AlertTitle>
        <AlertDescription>
          Tu cuenta no tiene ningún país asignado. Pide al admin que te asigne uno para ver pedidos.
        </AlertDescription>
      </Alert>
    );
  }
  if (!pais) {
    return (
      <Card className="max-w-md">
        <CardHeader>
          <CardTitle><h1 className="font-heading text-xl font-semibold">Elige un país</h1></CardTitle>
          <CardDescription>Verás y gestionarás solo los pedidos, bodegas y avisos de ese país.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          {asignados.map((opcion) => (
            <Button key={opcion} variant="outline" onClick={() => solicitarCambio(opcion)}>
              {NOMBRE_PAIS[opcion]}
            </Button>
          ))}
        </CardContent>
      </Card>
    );
  }
  return <div key={pais} className="contents">{children}</div>;
}

/** Selector del menú: solo los países asignados hoy; con uno solo, se muestra sin opciones. */
export function SelectorPais() {
  const ids = useId();
  const contexto = useContext(ContextoPais);
  if (!contexto || contexto.asignados.length === 0) return null;
  const { pais, asignados, solicitarCambio } = contexto;
  if (asignados.length === 1) {
    return (
      <p className="flex min-h-10 items-center gap-2 px-3 text-sm text-muted-foreground">
        <GlobeIcon aria-hidden="true" className="size-4" />
        País: <span className="font-medium text-foreground">{NOMBRE_PAIS[asignados[0]]}</span>
      </p>
    );
  }
  return (
    <div className="flex flex-col gap-1 px-1">
      <Label htmlFor={`${ids}-pais`} className="px-2 text-xs text-muted-foreground">País</Label>
      <Select value={pais ?? ''} onValueChange={(valor) => esPais(valor) && solicitarCambio(valor)}>
        <SelectTrigger id={`${ids}-pais`} className="min-h-10 w-full">
          <GlobeIcon aria-hidden="true" className="size-4" />
          <SelectValue placeholder="Elige un país" />
        </SelectTrigger>
        <SelectContent>
          {asignados.map((opcion) => (
            <SelectItem key={opcion} value={opcion}>{NOMBRE_PAIS[opcion]}</SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
