import { keepPreviousData, useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { useSesion } from '@/auth/SesionProvider';
import { esquemaBodega } from './esquemas';

// Pedidos, historial, archivos y flujos (contrato documentos/api/openapi.yaml 0.7.0-borrador). Las respuestas se
// validan con estos esquemas: si el backend cambia la forma sin cambiar el contrato, falla aquí y no en la pantalla.
// Nada se simula: la etapa solo cambia cuando el servidor responde, y la pantalla muestra lo que él devuelve.

// --- Esquemas del contrato -------------------------------------------------------------------------------------------

export const esquemaPersona = z.object({
  id: z.string(),
  nombre: z.string(),
  roles: z.array(z.string()),
});
export type Persona = z.infer<typeof esquemaPersona>;

export const NIVELES_URGENCIA = ['URGENTE', 'PRIORITARIO', 'NORMAL'] as const;
export type NivelUrgencia = (typeof NIVELES_URGENCIA)[number];

const esquemaUrgencia = z.object({
  nivel: z.enum(NIVELES_URGENCIA),
  horas: z.number().int(),
  activadaEn: z.string(),
  venceEn: z.string(),
});
export type Urgencia = z.infer<typeof esquemaUrgencia>;

const esquemaResumen = z.object({
  id: z.string(),
  numero: z.string(),
  nombre: z.string(),
  pais: z.string(),
  tipoFlujo: z.enum(['A', 'B']),
  etapaActual: z.number().int(),
  nombreEtapaActual: z.string(),
  responsableActual: esquemaPersona.nullish(),
  venceEn: z.string().nullish(),
  vencida: z.boolean(),
  motivoRetraso: z.string().nullish(),
  urgencia: esquemaUrgencia.nullish(),
  finalizado: z.boolean(),
});
export type PedidoResumen = z.infer<typeof esquemaResumen>;

export const ESTADOS_ETAPA = ['PENDIENTE', 'ACTUAL', 'COMPLETADA', 'DEVUELTA'] as const;
export type EstadoEtapa = (typeof ESTADOS_ETAPA)[number];
export type EstadoPlazo = 'DEFINIDO' | 'SIN_PLAZO' | 'PENDIENTE';

const esquemaEtapaEnLinea = z.object({
  numero: z.number().int(),
  nombre: z.string(),
  estado: z.enum(ESTADOS_ETAPA),
  rolResponsable: z.string().nullable(),
  estadoPlazo: z.enum(['DEFINIDO', 'SIN_PLAZO', 'PENDIENTE']),
  asignadoA: esquemaPersona.nullish(),
  // null: sin persona asignada; true: la asignada puede actuar; false: ya no puede (solo el admin actúa o reasigna).
  asignacionVigente: z.boolean().nullish(),
  entradaId: z.string().nullish(),
  entrada: z.string().nullish(),
  salida: z.string().nullish(),
  venceEn: z.string().nullish(),
  reentradas: z.number().int().nonnegative(),
  resumen: z.string().nullish(),
});
export type EtapaEnLinea = z.infer<typeof esquemaEtapaEnLinea>;

export const ACCIONES = [
  'ENVIAR',
  'APROBAR',
  'RECHAZAR',
  'DEVOLVER_POR_ERROR',
  'DEVOLVER_POR_PRODUCTO_NUEVO',
  'REPORTAR_DEMORA',
  'ELEGIR_BODEGA',
] as const;
export type AccionEtapa = (typeof ACCIONES)[number];

const esquemaAccionDisponible = z.object({
  accion: z.enum(ACCIONES),
  etapaDestino: z.number().int().nullable(),
  requiereObservacion: z.boolean(),
  requiereUrgencia: z.boolean(),
  requiereBodega: z.boolean(),
  requisitosDocumentales: z.enum(['DEFINIDOS', 'PENDIENTES']),
  documentosObligatorios: z.array(z.string()),
  documentosFaltantes: z.array(z.string()),
  puedeCompletarse: z.boolean(),
});
export type AccionDisponible = z.infer<typeof esquemaAccionDisponible>;

export const esquemaPedidoDetalle = esquemaResumen.extend({
  descripcion: z.string().nullish(),
  incluyeProductosNuevos: z.boolean().optional(),
  creadoPor: esquemaPersona,
  creadoEn: z.string(),
  versionFlujo: z.number().int(),
  bodega: esquemaBodega.nullish(),
  llegadaAduanaEn: z.string().nullish(),
  demoraAduana: z.object({
    reportadaPor: esquemaPersona,
    reportadaEn: z.string(),
    motivo: z.string(),
    nuevaFechaEstimadaLlegada: z.string(),
  }).nullish(),
  lineaDeTiempo: z.array(esquemaEtapaEnLinea),
  accionesDisponibles: z.array(esquemaAccionDisponible),
});
export type PedidoDetalle = z.infer<typeof esquemaPedidoDetalle>;

const esquemaPagina = z.object({
  elementos: z.array(esquemaResumen),
  pagina: z.number().int(),
  tamano: z.number().int(),
  total: z.number().int(),
});
export type PaginaPedidos = z.infer<typeof esquemaPagina>;

export const TIPOS_EVENTO = [
  'PEDIDO_CREADO',
  'ETAPA_ENVIADA',
  'ETAPA_APROBADA',
  'ETAPA_RECHAZADA',
  'PEDIDO_DEVUELTO',
  'DEMORA_REPORTADA',
  'BODEGA_ASIGNADA',
  'OBSERVACION_AGREGADA',
  'ARCHIVO_SUBIDO',
  'ETAPA_REASIGNADA',
  'PLAZO_VENCIDO',
  'PEDIDO_FINALIZADO',
] as const;
export type TipoEvento = (typeof TIPOS_EVENTO)[number];

export const esquemaEvento = z.object({
  id: z.string(),
  pedidoId: z.string(),
  numeroPedido: z.string().nullish(),
  etapa: z.number().int(),
  tipo: z.enum(TIPOS_EVENTO),
  usuario: esquemaPersona.nullable(),
  rolIntervencion: z.string().nullish(),
  detalle: z
    .object({
      etapaDestino: z.number().int().nullish(),
      motivoDevolucion: z.enum(['ERROR', 'PRODUCTO_NUEVO']).nullish(),
      urgenciaTerminada: z.boolean().nullish(),
      asignadoAnterior: z.string().nullish(),
      asignadoA: z.string().nullish(),
      fechaEstimadaAnterior: z.string().nullish(),
      fechaEstimadaSolicitada: z.string().nullish(),
      nuevaFechaEstimadaLlegada: z.string().nullish(),
      bodegaId: z.string().nullish(),
    })
    .nullish(),
  fecha: z.string(),
  observacion: z.string().nullish(),
  nivelUrgencia: z.enum(NIVELES_URGENCIA).nullish(),
  adjuntoIds: z.array(z.string()).nullish(),
});
export type EventoHistorial = z.infer<typeof esquemaEvento>;

export const esquemaAdjunto = z.object({
  id: z.string(),
  pedidoId: z.string(),
  etapa: z.number().int(),
  entradaId: z.string(),
  eventoId: z.string().nullish(),
  nombre: z.string(),
  tipoDocumento: z.string(),
  tamanoBytes: z.number().int().positive(),
  subidoPor: esquemaPersona,
  subidoEn: z.string(),
  estadoCopia: z.enum(['PENDIENTE', 'COPIADO', 'FALLIDO']),
});
export type Adjunto = z.infer<typeof esquemaAdjunto>;

const esquemaDefinicionFlujo = z.object({
  pais: z.string(),
  tipoFlujo: z.enum(['A', 'B']),
  version: z.number().int(),
  etapas: z.array(
    z.object({
      numero: z.number().int(),
      nombre: z.string(),
      rolResponsable: z.string().nullable(),
      estadoPlazo: z.enum(['DEFINIDO', 'SIN_PLAZO', 'PENDIENTE']),
      revisaEtapa: z.number().int().nullish(),
      plazo: z.object({ cantidad: z.number().int(), tipo: z.string() }).nullish(),
      acciones: z.array(
        z.object({
          accion: z.enum(ACCIONES),
          etapaDestino: z.number().int().nullable(),
          requiereObservacion: z.boolean(),
          requiereUrgencia: z.boolean(),
          requiereBodega: z.boolean(),
          requisitosDocumentales: z.enum(['DEFINIDOS', 'PENDIENTES']),
          documentosObligatorios: z.array(z.string()),
        }),
      ),
      automatica: z.boolean(),
      registraLlegada: z.boolean(),
    }),
  ),
});
export type DefinicionFlujo = z.infer<typeof esquemaDefinicionFlujo>;

// --- Claves de consulta ----------------------------------------------------------------------------------------------
// Separadas por sesión, como las de usuarios: al cambiar de cuenta no se mezcla nada de la anterior.

export interface FiltrosPedidos {
  texto?: string;
  etapa?: number;
  vencido?: boolean;
  pagina: number;
  tamano: number;
}

export const clavesPedidos = {
  listas: (sesion: number) => ['sesion', sesion, 'pedidos', 'lista'] as const,
  lista: (sesion: number, filtros: FiltrosPedidos) => ['sesion', sesion, 'pedidos', 'lista', filtros] as const,
  bandejas: (sesion: number) => ['sesion', sesion, 'pedidos', 'bandeja'] as const,
  bandeja: (sesion: number, pagina: number) => ['sesion', sesion, 'pedidos', 'bandeja', pagina] as const,
  detalle: (sesion: number, id: string) => ['sesion', sesion, 'pedidos', 'detalle', id] as const,
  historial: (sesion: number, id: string) => ['sesion', sesion, 'pedidos', 'historial', id] as const,
  archivos: (sesion: number, id: string) => ['sesion', sesion, 'pedidos', 'archivos', id] as const,
  archivosDeEtapa: (sesion: number, id: string, etapa: number) =>
    ['sesion', sesion, 'pedidos', 'archivos', id, etapa] as const,
  flujo: (sesion: number, pais: string, tipo: string, version: number) =>
    ['sesion', sesion, 'flujos', pais, tipo, version] as const,
};

const id = (valor: string) => encodeURIComponent(valor);

// --- Consultas -------------------------------------------------------------------------------------------------------

export function usePedidos(filtros: FiltrosPedidos) {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: clavesPedidos.lista(sesionId, filtros),
    queryFn: ({ signal }) => {
      const parametros = new URLSearchParams({ pagina: String(filtros.pagina), tamano: String(filtros.tamano) });
      if (filtros.texto) parametros.set('texto', filtros.texto);
      if (filtros.etapa) parametros.set('etapa', String(filtros.etapa));
      if (filtros.vencido) parametros.set('vencido', 'true');
      return pedir(`/pedidos?${parametros}`, { esquema: esquemaPagina, signal });
    },
    // Al paginar o filtrar, la tabla anterior queda visible mientras llega la nueva.
    placeholderData: keepPreviousData,
  });
}

/** Bandeja de trabajo según P-06: lo que la persona puede atender ahora (el admin ve todo lo que está en curso). */
export function useBandeja(pagina: number) {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: clavesPedidos.bandeja(sesionId, pagina),
    queryFn: ({ signal }) =>
      pedir(`/pedidos/bandeja?pagina=${pagina}&tamano=100`, { esquema: esquemaPagina, signal }),
    placeholderData: keepPreviousData,
  });
}

export function usePedido(pedidoId: string) {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: clavesPedidos.detalle(sesionId, pedidoId),
    queryFn: ({ signal }) => pedir(`/pedidos/${id(pedidoId)}`, { esquema: esquemaPedidoDetalle, signal }),
    // El estado de un pedido lo cambian otras personas: se vuelve a leer pronto.
    staleTime: 10_000,
  });
}

export function useHistorial(pedidoId: string) {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: clavesPedidos.historial(sesionId, pedidoId),
    queryFn: ({ signal }) =>
      pedir(`/pedidos/${id(pedidoId)}/historial`, { esquema: z.array(esquemaEvento), signal }),
    staleTime: 10_000,
  });
}

export function useAdjuntosDeEtapa(pedidoId: string, etapa: number) {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: clavesPedidos.archivosDeEtapa(sesionId, pedidoId, etapa),
    queryFn: ({ signal }) =>
      pedir(`/pedidos/${id(pedidoId)}/etapas/${etapa}/archivos`, { esquema: z.array(esquemaAdjunto), signal }),
    staleTime: 10_000,
    refetchInterval: (consulta) => consulta.state.data?.some(a => a.estadoCopia === 'PENDIENTE') ? 30_000 : false,
    refetchIntervalInBackground: false,
  });
}

/**
 * Archivos de varias etapas (los que aparecen en el historial). Comparte las claves con useAdjuntosDeEtapa, así que
 * reutiliza lo ya cargado y se invalida junto con ello. Devuelve los adjuntos conocidos por id.
 */
export function useAdjuntosDeEtapas(pedidoId: string, etapas: number[]) {
  const { pedir, sesionId } = useSesion();
  return useQueries({
    queries: etapas.map((etapa) => ({
      queryKey: clavesPedidos.archivosDeEtapa(sesionId, pedidoId, etapa),
      queryFn: ({ signal }: { signal: AbortSignal }) =>
        pedir(`/pedidos/${id(pedidoId)}/etapas/${etapa}/archivos`, { esquema: z.array(esquemaAdjunto), signal }),
      staleTime: 10_000,
    })),
    combine: (resultados) => {
      const porId = new Map<string, Adjunto>();
      for (const resultado of resultados) for (const adjunto of resultado.data ?? []) porId.set(adjunto.id, adjunto);
      return porId;
    },
  });
}

/** Definición de la versión del flujo con la que se creó el pedido (ADR-0004): nombres, roles y revisión (P-17). */
export function useDefinicionFlujo(pais: string, tipo: string, version: number | undefined) {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: clavesPedidos.flujo(sesionId, pais, tipo, version ?? 0),
    queryFn: ({ signal }) =>
      pedir(`/flujos/${id(pais)}/${id(tipo)}${version ? `?version=${version}` : ''}`, {
        esquema: esquemaDefinicionFlujo,
        signal,
      }),
    // Una versión publicada no cambia (ADR-0004); la vigente se relee cada tanto.
    staleTime: version ? Infinity : 5 * 60 * 1000,
  });
}

// --- Mutaciones ------------------------------------------------------------------------------------------------------
// Ninguna se reintenta sola (main.tsx) ni cambia la pantalla antes de que responda el servidor: un 409 o un 422 lo
// resuelve la persona con el estado vigente.

export interface DatosNuevoPedido {
  tipoFlujo: 'A';
  nombre: string;
  descripcion?: string;
  incluyeProductosNuevos: boolean;
}

export function useCrearPedido() {
  const { pedir, sesionId } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: (datos: DatosNuevoPedido) =>
      pedir('/pedidos', { metodo: 'POST', cuerpo: { ...datos, pais: 'EC' }, esquema: esquemaPedidoDetalle }),
    onSuccess: (pedido) => {
      consultas.setQueryData(clavesPedidos.detalle(sesionId, pedido.id), pedido);
      void consultas.invalidateQueries({ queryKey: clavesPedidos.listas(sesionId) });
      void consultas.invalidateQueries({ queryKey: clavesPedidos.bandejas(sesionId) });
    },
  });
}

/** Tras cualquier cambio de un pedido: su detalle, su historial, sus archivos y las listas que lo muestran. */
export function useRefrescarPedido() {
  const { sesionId } = useSesion();
  const consultas = useQueryClient();
  return (pedidoId: string, detalle?: PedidoDetalle) => {
    if (detalle) consultas.setQueryData(clavesPedidos.detalle(sesionId, pedidoId), detalle);
    else void consultas.invalidateQueries({ queryKey: clavesPedidos.detalle(sesionId, pedidoId) });
    void consultas.invalidateQueries({ queryKey: ['notificaciones', sesionId] });
    void consultas.invalidateQueries({ queryKey: clavesPedidos.historial(sesionId, pedidoId) });
    void consultas.invalidateQueries({ queryKey: clavesPedidos.archivos(sesionId, pedidoId) });
    void consultas.invalidateQueries({ queryKey: clavesPedidos.listas(sesionId) });
    void consultas.invalidateQueries({ queryKey: clavesPedidos.bandejas(sesionId) });
  };
}

export interface DatosAccion {
  pedidoId: string;
  accion: AccionEtapa;
  etapaEsperada: number;
  observacion?: string;
  nivelUrgencia?: NivelUrgencia;
  nuevaFechaEstimadaLlegada?: string;
  bodegaId?: string;
  adjuntoIds: string[];
}

export function useEjecutarAccion() {
  const { pedir } = useSesion();
  const refrescar = useRefrescarPedido();
  return useMutation({
    mutationFn: ({ pedidoId, ...cuerpo }: DatosAccion) =>
      pedir(`/pedidos/${id(pedidoId)}/acciones`, { metodo: 'POST', cuerpo, esquema: esquemaPedidoDetalle }),
    onSuccess: (pedido, { pedidoId }) => refrescar(pedidoId, pedido),
    // 409 ETAPA_CAMBIO u otro rechazo: se trae el estado vigente; nada se reenvía.
    onError: (_error, { pedidoId }) => refrescar(pedidoId),
  });
}

export interface DatosReasignacion {
  pedidoId: string;
  etapa: number;
  usuarioId: string;
  motivo?: string;
}

export function useReasignarEtapa() {
  const { pedir } = useSesion();
  const refrescar = useRefrescarPedido();
  return useMutation({
    mutationFn: ({ pedidoId, etapa, ...cuerpo }: DatosReasignacion) =>
      pedir(`/pedidos/${id(pedidoId)}/etapas/${etapa}/asignacion`, {
        metodo: 'PUT',
        cuerpo,
        esquema: esquemaPedidoDetalle,
      }),
    onSuccess: (pedido, { pedidoId }) => refrescar(pedidoId, pedido),
    onError: (_error, { pedidoId }) => refrescar(pedidoId),
  });
}

export interface DatosObservacion {
  pedidoId: string;
  etapaEsperada: number;
  entradaEsperadaId: string;
  texto: string;
  adjuntoIds: string[];
}

export function useAgregarObservacion() {
  const { pedir } = useSesion();
  const refrescar = useRefrescarPedido();
  return useMutation({
    mutationFn: ({ pedidoId, ...cuerpo }: DatosObservacion) =>
      pedir(`/pedidos/${id(pedidoId)}/observaciones`, { metodo: 'POST', cuerpo, esquema: esquemaEvento }),
    // Se recargan también detalle y bandejas; no se deriva ningún estado en el navegador.
    onSuccess: (_evento, { pedidoId }) => refrescar(pedidoId),
    onError: (_error, { pedidoId }) => refrescar(pedidoId),
  });
}

export interface DatosSubida {
  pedidoId: string;
  etapa: number;
  entradaEsperadaId: string;
  tipoDocumento: string;
  archivo: File;
}

export function useSubirArchivo() {
  const { pedir } = useSesion();
  const refrescar = useRefrescarPedido();
  return useMutation({
    mutationFn: ({ pedidoId, etapa, entradaEsperadaId, tipoDocumento, archivo }: DatosSubida) => {
      const formulario = new FormData();
      formulario.set('archivo', archivo, archivo.name);
      formulario.set('tipoDocumento', tipoDocumento);
      formulario.set('entradaEsperadaId', entradaEsperadaId);
      return pedir(`/pedidos/${id(pedidoId)}/etapas/${etapa}/archivos`, {
        metodo: 'POST',
        cuerpo: formulario,
        esquema: esquemaAdjunto,
      });
    },
    // Los documentos que faltan para cada acción (accionesDisponibles) dependen de los archivos: se relee el detalle.
    onSettled: (_adjunto, _error, { pedidoId }) => refrescar(pedidoId),
  });
}

/** Descarga el contenido a través del servidor (P-17). El servidor decide el permiso y comprueba la huella. */
export function useDescargarArchivo() {
  const { pedir } = useSesion();
  return useMutation({
    mutationFn: async (adjunto: Adjunto) => {
      const contenido = await pedir<Blob>(`/archivos/${id(adjunto.id)}/contenido`, { formato: 'blob' });
      const direccion = URL.createObjectURL(contenido);
      const enlace = document.createElement('a');
      enlace.href = direccion;
      enlace.download = adjunto.nombre;
      enlace.rel = 'noopener';
      document.body.append(enlace);
      enlace.click();
      enlace.remove();
      // Se libera después: el navegador ya tomó la descarga.
      setTimeout(() => URL.revokeObjectURL(direccion), 30_000);
    },
  });
}
