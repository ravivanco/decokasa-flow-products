import { keepPreviousData, useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { clavesEstadisticas } from './estadisticas';
import { useSesion } from '@/auth/SesionProvider';
import { clavesAvisos } from './avisos';
import { opcionesSeguimiento } from './actualizacion';
import { esquemaBodega } from './esquemas';
import { type CodigoPais, segmentoPais, usePaisActivo } from '@/pais/PaisProvider';

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
  // Paso 5.3: la urgencia vigente ya venció. Lo decide el servidor con su reloj; aquí no se calcula.
  vencida: z.boolean().optional(),
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
  // V16 (contrato 0.14.0): al menos un documento de la entrada actual, de cualquier tipo (A9 y A10). Ausente = false.
  exigeAlgunDocumento: z.boolean().optional(),
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
  'MOTIVO_RETRASO_REGISTRADO',
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
      claseVencimiento: z.enum(['ETAPA', 'URGENCIA']).nullish(),
      venceEn: z.string().nullish(),
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
          exigeAlgunDocumento: z.boolean().optional(),
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
  /** Fase 6: el servidor filtra por flujo; con `etapa`, el número es de ese flujo (B2 no es A2). */
  tipoFlujo?: 'A' | 'B';
  etapa?: number;
  vencido?: boolean;
  pagina: number;
  tamano: number;
}

/** Filtros de la bandeja del dashboard (paso 5.3). Los aplica el servidor: solo true filtra. */
export interface FiltroBandeja {
  vencido?: boolean;
  conUrgencia?: boolean;
}

// Paso 7.3: los datos de pedidos van además por país activo (segmentoPais). Al cambiar de país o al revocarlo se
// cancelan y se borran las claves del anterior (PaisProvider), así que ninguna respuesta tardía se muestra.
export const clavesPedidos = {
  listas: (sesion: number, pais: CodigoPais) => ['sesion', sesion, 'pedidos', segmentoPais(pais), 'lista'] as const,
  lista: (sesion: number, pais: CodigoPais, filtros: FiltrosPedidos) =>
    ['sesion', sesion, 'pedidos', segmentoPais(pais), 'lista', filtros] as const,
  bandejas: (sesion: number, pais: CodigoPais) =>
    ['sesion', sesion, 'pedidos', segmentoPais(pais), 'bandeja'] as const,
  bandeja: (sesion: number, pais: CodigoPais, pagina: number, filtro: FiltroBandeja = {}, tamano = 100) =>
    [
      'sesion',
      sesion,
      'pedidos',
      segmentoPais(pais),
      'bandeja',
      { pagina, tamano, vencido: Boolean(filtro.vencido), conUrgencia: Boolean(filtro.conUrgencia) },
    ] as const,
  cronograma: (sesion: number, pais: CodigoPais, id: string) =>
    ['sesion', sesion, 'pedidos', segmentoPais(pais), 'cronograma', id] as const,
  detalle: (sesion: number, pais: CodigoPais, id: string) =>
    ['sesion', sesion, 'pedidos', segmentoPais(pais), 'detalle', id] as const,
  historial: (sesion: number, pais: CodigoPais, id: string) =>
    ['sesion', sesion, 'pedidos', segmentoPais(pais), 'historial', id] as const,
  archivos: (sesion: number, pais: CodigoPais, id: string) =>
    ['sesion', sesion, 'pedidos', segmentoPais(pais), 'archivos', id] as const,
  archivosDeEtapa: (sesion: number, pais: CodigoPais, id: string, etapa: number) =>
    ['sesion', sesion, 'pedidos', segmentoPais(pais), 'archivos', id, etapa] as const,
  flujo: (sesion: number, pais: string, tipo: string, version: number) =>
    ['sesion', sesion, 'flujos', pais, tipo, version] as const,
  documentos: (sesion: number, pais: CodigoPais, id: string) =>
    ['sesion', sesion, 'pedidos', segmentoPais(pais), 'documentos', id] as const,
};

// Documentos del pedido vistos desde DECOKASA (ADR-0028): sin IDs ni enlaces de Drive. Abrir vuelve a autorizar.
export const esquemaDocumentoPedido = z.object({
  id: z.string(),
  pedidoId: z.string(),
  etapa: z.number().int(),
  entradaId: z.string(),
  entradaEn: z.string(),
  nombre: z.string(),
  tipoDocumento: z.string(),
  tipoContenido: z.string().nullable(),
  tamanoBytes: z.number().int().positive(),
  subidoPor: z.object({ id: z.string(), nombre: z.string(), roles: z.array(z.string()) }),
  subidoEn: z.string(),
  estadoCopia: z.enum(['PENDIENTE', 'COPIADO', 'FALLIDO']),
  disponibilidad: z.enum(['LOCAL', 'DRIVE', 'NO_DISPONIBLE']),
  puedeAbrir: z.boolean(),
  /** ADR-0029: entregado a la etapa actual por la última transición. */
  vigente: z.boolean(),
  presentaciones: z.array(
    z.object({ eventoId: z.string(), tipo: z.string(), etapa: z.number().int(), fecha: z.string() }),
  ),
});
export type DocumentoPedido = z.infer<typeof esquemaDocumentoPedido>;

const id = (valor: string) => encodeURIComponent(valor);

// --- Consultas -------------------------------------------------------------------------------------------------------

export function usePedidos(filtros: FiltrosPedidos, habilitada = true) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesPedidos.lista(sesionId, pais, filtros),
    enabled: habilitada,
    queryFn: ({ signal }) => {
      const parametros = new URLSearchParams({
        pais,
        pagina: String(filtros.pagina),
        tamano: String(filtros.tamano),
      });
      if (filtros.texto) parametros.set('texto', filtros.texto);
      if (filtros.tipoFlujo) parametros.set('tipoFlujo', filtros.tipoFlujo);
      if (filtros.etapa) parametros.set('etapa', String(filtros.etapa));
      if (filtros.vencido) parametros.set('vencido', 'true');
      return pedir(`/pedidos?${parametros}`, { esquema: esquemaPagina, signal });
    },
    // Al paginar o filtrar, la tabla anterior queda visible mientras llega la nueva.
    placeholderData: keepPreviousData,
    ...opcionesSeguimiento,
  });
}

function parametrosBandeja(pais: CodigoPais, pagina: number, tamano: number, filtro: FiltroBandeja) {
  const parametros = new URLSearchParams({ pais, pagina: String(pagina), tamano: String(tamano) });
  if (filtro.vencido) parametros.set('vencido', 'true');
  if (filtro.conUrgencia) parametros.set('conUrgencia', 'true');
  return parametros;
}

/**
 * Bandeja de trabajo según P-06: lo que la persona puede atender ahora (el admin ve todo lo que está en curso). Los
 * filtros del dashboard (paso 5.3) los aplica el servidor, no la página cargada.
 */
export function useBandeja(pagina: number, filtro: FiltroBandeja = {}, tamano = 100) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesPedidos.bandeja(sesionId, pais, pagina, filtro, tamano),
    queryFn: ({ signal }) =>
      pedir(`/pedidos/bandeja?${parametrosBandeja(pais, pagina, tamano, filtro)}`, { esquema: esquemaPagina, signal }),
    placeholderData: keepPreviousData,
    ...opcionesSeguimiento,
  });
}

const FILTROS_CONTEO: FiltroBandeja[] = [{}, { vencido: true }, { conUrgencia: true }];

/**
 * Totales de la bandeja para las pestañas del dashboard (paso 5.3): tres lecturas de una sola fila cada una; los
 * cuenta el servidor con sus filtros. Indefinido mientras no llega la respuesta.
 */
export function useConteosBandeja() {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQueries({
    queries: FILTROS_CONTEO.map((filtro) => ({
      queryKey: clavesPedidos.bandeja(sesionId, pais, 0, filtro, 1),
      queryFn: ({ signal }: { signal: AbortSignal }) =>
        pedir(`/pedidos/bandeja?${parametrosBandeja(pais, 0, 1, filtro)}`, { esquema: esquemaPagina, signal }),
      ...opcionesSeguimiento,
    })),
    combine: (resultados) => ({
      pendientes: resultados[0].data?.total,
      vencidos: resultados[1].data?.total,
      conUrgencia: resultados[2].data?.total,
      error: resultados.some((resultado) => resultado.isError),
    }),
  });
}

export function usePedido(pedidoId: string) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesPedidos.detalle(sesionId, pais, pedidoId),
    queryFn: ({ signal }) => pedir(`/pedidos/${id(pedidoId)}`, { esquema: esquemaPedidoDetalle, signal }),
    // El estado de un pedido lo cambian otras personas: se vuelve a leer pronto y cada intervalo (paso 5.4).
    staleTime: 10_000,
    ...opcionesSeguimiento,
  });
}

/** Documentos del pedido (P-13 para verlos; `puedeAbrir` según P-17 ampliada, ADR-0028). */
export function useDocumentosPedido(pedidoId: string) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesPedidos.documentos(sesionId, pais, pedidoId),
    queryFn: ({ signal }) =>
      pedir(`/pedidos/${id(pedidoId)}/documentos`, { esquema: z.array(esquemaDocumentoPedido), signal }),
    staleTime: 10_000,
    ...opcionesSeguimiento,
  });
}

/** Tipos que DECOKASA muestra en su visor; el resto se descarga (ADR-0028). */
export const TIPOS_VISIBLES = ['application/pdf', 'image/png', 'image/jpeg'] as const;

/**
 * Contenido de un documento para el visor de DECOKASA: se pide al backend con la sesión en la cabecera (nunca un JWT en
 * la URL) y se devuelve una URL blob: local con el tipo canónico validado. Quien la recibe debe revocarla al cerrar.
 */
export function useVerArchivo() {
  const { pedir } = useSesion();
  return useMutation({
    mutationFn: async (documento: { id: string; tipoContenido: string | null }) => {
      const tipo = TIPOS_VISIBLES.find((visible) => visible === documento.tipoContenido);
      if (!tipo) throw new Error('Este tipo de documento no se puede ver en DECOKASA; descárgalo.');
      const contenido = await pedir<Blob>(`/archivos/${id(documento.id)}/contenido`, { formato: 'blob' });
      return { url: URL.createObjectURL(new Blob([contenido], { type: tipo })), tipo };
    },
  });
}

export function useHistorial(pedidoId: string) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesPedidos.historial(sesionId, pais, pedidoId),
    queryFn: ({ signal }) =>
      pedir(`/pedidos/${id(pedidoId)}/historial`, { esquema: z.array(esquemaEvento), signal }),
    staleTime: 10_000,
    ...opcionesSeguimiento,
  });
}

/**
 * El historial solo si ya está en caché (mejora visual del 2026-10-27): el botón «Ver historial» muestra la cantidad de
 * eventos sin descargarlo; la descarga ocurre al abrir el panel (useHistorial).
 */
export function useHistorialEnCache(pedidoId: string) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesPedidos.historial(sesionId, pais, pedidoId),
    queryFn: ({ signal }) =>
      pedir(`/pedidos/${id(pedidoId)}/historial`, { esquema: z.array(esquemaEvento), signal }),
    enabled: false,
  });
}

export function useAdjuntosDeEtapa(pedidoId: string, etapa: number) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesPedidos.archivosDeEtapa(sesionId, pais, pedidoId, etapa),
    queryFn: ({ signal }) =>
      pedir(`/pedidos/${id(pedidoId)}/etapas/${etapa}/archivos`, { esquema: z.array(esquemaAdjunto), signal }),
    staleTime: 10_000,
    // Paso 5.4: los archivos del detalle visible se refrescan con el intervalo común (antes, solo con copias a Drive
    // pendientes). Mismas pausas que las demás consultas de seguimiento.
    ...opcionesSeguimiento,
  });
}

/**
 * Archivos de varias etapas (los que aparecen en el historial). Comparte las claves con useAdjuntosDeEtapa, así que
 * reutiliza lo ya cargado y se invalida junto con ello. Devuelve los adjuntos conocidos por id.
 */
export function useAdjuntosDeEtapas(pedidoId: string, etapas: number[]) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQueries({
    queries: etapas.map((etapa) => ({
      queryKey: clavesPedidos.archivosDeEtapa(sesionId, pais, pedidoId, etapa),
      queryFn: ({ signal }: { signal: AbortSignal }) =>
        pedir(`/pedidos/${id(pedidoId)}/etapas/${etapa}/archivos`, { esquema: z.array(esquemaAdjunto), signal }),
      staleTime: 10_000,
      ...opcionesSeguimiento,
    })),
    combine: (resultados) => {
      const porId = new Map<string, Adjunto>();
      for (const resultado of resultados) for (const adjunto of resultado.data ?? []) porId.set(adjunto.id, adjunto);
      return porId;
    },
  });
}

export const PROCEDENCIAS_FECHA = ['REAL', 'LLEGADA_REAL', 'LLEGADA_ESTIMADA', 'CALCULADA', 'PROYECTADA'] as const;
export type ProcedenciaFecha = (typeof PROCEDENCIAS_FECHA)[number];
export const ESTADOS_CRONOGRAMA = ['REAL', 'EN_CURSO', 'EXCEDIDA', 'ESTIMADA', 'SIN_ESTIMAR'] as const;
export type EstadoCronograma = (typeof ESTADOS_CRONOGRAMA)[number];
export const BASES_DURACION = ['PLAZO', 'REFERENCIA', 'SIN_PLAZO', 'SIN_DATO'] as const;
export type BaseDuracion = (typeof BASES_DURACION)[number];

const esquemaFaltante = z.object({ numero: z.number().int(), nombre: z.string() });

// Cronograma (contrato 0.25.0-borrador, ADR-0030): planificación original, estimación actual y fechas reales por
// separado. Las duraciones de referencia sirven para estimar, no son plazos. Aquí no se calcula ninguna fecha. Los
// campos nuevos admiten ausencia para leer respuestas anteriores a 0.25.0.
const esquemaCronograma = z.object({
  pedidoId: z.string(),
  fechaPedido: z.string(),
  calculadoEn: z.string().nullish(),
  reglas: z.string().nullish(),
  meta: z.object({
    dias: z.number().int(),
    origen: z.enum(['PROPUESTA', 'ADMIN']),
    pendienteDelCliente: z.boolean(),
  }),
  llegada: z.object({ fecha: z.string(), procedencia: z.enum(PROCEDENCIAS_FECHA) }).nullish(),
  original: z
    .object({
      origen: z.enum(['GUARDADA', 'RECONSTRUIDA']),
      reglas: z.string(),
      calculadaEn: z.string().nullish(),
      llegada: z.string().nullish(),
      finalizacion: z.string().nullish(),
    })
    .nullish(),
  diferenciaContraOriginal: z.number().int().nullish(),
  finalizacionEstimada: z.string().nullish(),
  totalDias: z.number().int().nullish(),
  diferenciaContraMeta: z.number().int().nullish(),
  calculable: z.boolean(),
  faltantes: z.array(esquemaFaltante),
  faltantesFinalizacion: z.array(esquemaFaltante).nullish(),
  etapas: z.array(
    z.object({
      numero: z.number().int(),
      nombre: z.string(),
      fechaOriginal: z.string().nullish(),
      fechaEstimada: z.string().nullish(),
      fechaReal: z.string().nullish(),
      movidaAlLunes: z.boolean(),
      estado: z.enum(ESTADOS_CRONOGRAMA),
      procedencia: z.enum(PROCEDENCIAS_FECHA).nullish(),
      plazoPendiente: z.boolean(),
      duracion: z
        .object({
          base: z.enum(BASES_DURACION),
          cantidad: z.number().int().nullish(),
          tipo: z.enum(['HORAS_HABILES', 'DIAS_HABILES', 'DIAS_CORRIDOS']).nullish(),
          fuente: z.enum(['CALCULADORA_ORIGINAL', 'REQUERIMIENTOS_4_5']).nullish(),
        })
        .nullish(),
    }),
  ),
});
export type Cronograma = z.infer<typeof esquemaCronograma>;

export function useCronograma(pedidoId: string) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesPedidos.cronograma(sesionId, pais, pedidoId),
    queryFn: ({ signal }) =>
      pedir(`/pedidos/${id(pedidoId)}/cronograma`, { esquema: esquemaCronograma, signal }),
    staleTime: 10_000,
    ...opcionesSeguimiento,
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

const TIPOS_DE_FLUJO = ['A', 'B'] as const;

/**
 * Definiciones vigentes del país activo (paso 7.3; antes, solo Ecuador) de los flujos A y B (fase 6), con la misma
 * clave que {@link useDefinicionFlujo}: una
 * consulta por flujo, compartida por todas las pantallas. Las tablas eligen la de cada pedido por su `tipoFlujo`, así
 * B se dibuja con sus doce etapas y A con sus once. Indefinida mientras no llega o si falló.
 */
export function useDefinicionesVigentes() {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQueries({
    queries: TIPOS_DE_FLUJO.map((tipo) => ({
      queryKey: clavesPedidos.flujo(sesionId, pais, tipo, 0),
      queryFn: ({ signal }: { signal: AbortSignal }) =>
        pedir(`/flujos/${pais}/${tipo}`, { esquema: esquemaDefinicionFlujo, signal }),
      staleTime: 5 * 60 * 1000,
    })),
    combine: (resultados) => ({
      A: resultados[0].data,
      B: resultados[1].data,
      cargando: resultados.some((resultado) => resultado.isPending),
      error: resultados.some((resultado) => resultado.isError),
    }),
  });
}

// --- Mutaciones ------------------------------------------------------------------------------------------------------
// Ninguna se reintenta sola (main.tsx) ni cambia la pantalla antes de que responda el servidor: un 409 o un 422 lo
// resuelve la persona con el estado vigente.

export interface DatosNuevoPedido {
  /** A lo crea el admin y B Compras China (paso 6.2): decide el servidor con el rol de la primera etapa. */
  tipoFlujo: 'A' | 'B';
  nombre: string;
  descripcion?: string;
  incluyeProductosNuevos: boolean;
}

export function useCrearPedido() {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  const consultas = useQueryClient();
  return useMutation({
    // Paso 7.3: siempre en el país activo; el servidor exige que esté asignado (403 si no).
    mutationFn: (datos: DatosNuevoPedido) =>
      pedir('/pedidos', { metodo: 'POST', cuerpo: { ...datos, pais }, esquema: esquemaPedidoDetalle }),
    onSuccess: (pedido) => {
      if (pedido.pais === pais) consultas.setQueryData(clavesPedidos.detalle(sesionId, pais, pedido.id), pedido);
      void consultas.invalidateQueries({ queryKey: clavesPedidos.listas(sesionId, pais) });
      void consultas.invalidateQueries({ queryKey: clavesPedidos.bandejas(sesionId, pais) });
      void consultas.invalidateQueries({ queryKey: clavesEstadisticas.todas(sesionId) });
      void consultas.invalidateQueries({ queryKey: clavesEstadisticas.alertas(sesionId) });
      void consultas.invalidateQueries({ queryKey: clavesAvisos.todos(sesionId) });
    },
  });
}

/** Tras cualquier cambio de un pedido: su detalle, su historial, sus archivos y las listas que lo muestran. */
export function useRefrescarPedido() {
  const { sesionId } = useSesion();
  const pais = usePaisActivo();
  const consultas = useQueryClient();
  return (pedidoId: string, detalle?: PedidoDetalle) => {
    // Una respuesta de otro país (país cambiado mientras tanto) no se guarda en la caché del país activo.
    if (detalle && detalle.pais === pais) {
      consultas.setQueryData(clavesPedidos.detalle(sesionId, pais, pedidoId), detalle);
    } else void consultas.invalidateQueries({ queryKey: clavesPedidos.detalle(sesionId, pais, pedidoId) });
    void consultas.invalidateQueries({ queryKey: clavesPedidos.historial(sesionId, pais, pedidoId) });
    void consultas.invalidateQueries({ queryKey: clavesPedidos.cronograma(sesionId, pais, pedidoId) });
    void consultas.invalidateQueries({ queryKey: clavesPedidos.archivos(sesionId, pais, pedidoId) });
    void consultas.invalidateQueries({ queryKey: clavesPedidos.listas(sesionId, pais) });
    void consultas.invalidateQueries({ queryKey: clavesPedidos.bandejas(sesionId, pais) });
    // ADR-0029: el Inicio y la campana se recalculan con el cambio.
    void consultas.invalidateQueries({ queryKey: clavesEstadisticas.todas(sesionId) });
    void consultas.invalidateQueries({ queryKey: clavesEstadisticas.alertas(sesionId) });
      void consultas.invalidateQueries({ queryKey: clavesAvisos.todos(sesionId) });
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

/** Búsqueda exacta por número (requisito 4.2 #17; contrato GET /pedidos/buscar). La pide la persona al enviar. */
export function useBuscarPedido() {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  const consultas = useQueryClient();
  return useMutation({
    // Paso 7.3: solo en el país activo; un número de otro país responde como inexistente (404).
    mutationFn: (numero: string) =>
      pedir(`/pedidos/buscar?numero=${encodeURIComponent(numero)}&pais=${pais}`, { esquema: esquemaPedidoDetalle }),
    onSuccess: (pedido) => {
      if (pedido.pais === pais) consultas.setQueryData(clavesPedidos.detalle(sesionId, pais, pedido.id), pedido);
    },
  });
}

export interface DatosMotivo {
  pedidoId: string;
  etapa: number;
  entradaEsperadaId: string;
  motivo: string;
}

/**
 * Motivo del retraso (F5-D3, provisional del equipo): quien puede actuar en la etapa (P-06) o el admin, con un
 * vencimiento activo. Cada registro o corrección queda en el historial; no envía correo. El servidor decide.
 */
export function useRegistrarMotivo() {
  const { pedir } = useSesion();
  const refrescar = useRefrescarPedido();
  return useMutation({
    mutationFn: ({ pedidoId, etapa, ...cuerpo }: DatosMotivo) =>
      pedir(`/pedidos/${id(pedidoId)}/etapas/${etapa}/motivo-retraso`, {
        metodo: 'PUT',
        cuerpo,
        esquema: esquemaEvento,
      }),
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

/** Descarga el contenido a través del servidor (P-17). El servidor decide el permiso y comprueba la huella. */
export function useDescargarArchivo() {
  const { pedir } = useSesion();
  return useMutation({
    mutationFn: async (adjunto: Pick<Adjunto, 'id' | 'nombre'>) => {
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
