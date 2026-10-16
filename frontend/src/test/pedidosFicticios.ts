import { http, HttpResponse } from 'msw';

// Pedidos, etapas, archivos e historial ficticios con la forma del contrato 0.7.0-borrador (documentos/api/openapi.yaml)
// y una API simulada con estado: las respuestas cambian solo cuando la prueba lo indica, para comprobar que la
// pantalla muestra lo que devuelve el servidor y nunca avanza una etapa por su cuenta. Nunca datos reales; los nombres
// de A5 a A10 son solo de prueba.

export const PEDIDO_ID = '0193f0a0-0000-7000-8000-000000000001';
export const NUMERO = 'DK-EC-2026-0001';

const ETAPAS_A: [string, string | null][] = [
  ['Crear solicitud de pedido', 'admin'],
  ['Codificación', 'marketing'],
  ['Revisión y aprobación', 'asistente_compras'],
  ['Análisis y gestión', 'compras_china'],
  ['Fabricación', 'compras_china'],
  ['Embarque', 'compras_china'],
  ['Tránsito', 'logistica'],
  ['Aduana', 'logistica'],
  ['Bodega', 'logistica'],
  ['Recepción', 'logistica'],
  ['Fin', null],
];

export const entradaDe = (etapa: number, vez = 0) =>
  `0193f0a0-0000-7000-9000-${String(etapa * 100 + vez).padStart(12, '0')}`;

export const persona = (id: string, nombre: string, roles: string[]) => ({ id, nombre, roles });

export const ADMIN_PERSONA = persona('00000000-0000-4000-8000-000000000010', 'Admin Prueba', ['admin']);

export type AccionFicticia = ReturnType<typeof accion>;

export function accion(
  nombre: string,
  etapaDestino: number | null,
  extra: Partial<{
    requiereObservacion: boolean;
    requiereUrgencia: boolean;
    requisitosDocumentales: 'DEFINIDOS' | 'PENDIENTES';
    documentosObligatorios: string[];
    documentosFaltantes: string[];
    puedeCompletarse: boolean;
  }> = {},
) {
  return {
    accion: nombre,
    etapaDestino,
    requiereObservacion: false,
    requiereUrgencia: false,
    requisitosDocumentales: 'DEFINIDOS' as 'DEFINIDOS' | 'PENDIENTES',
    documentosObligatorios: [] as string[],
    documentosFaltantes: [] as string[],
    puedeCompletarse: true,
    ...extra,
  };
}

export interface OpcionesPedido {
  etapaActual?: number;
  acciones?: AccionFicticia[];
  resumenes?: Record<number, string | null>;
  asignaciones?: Record<number, { persona: ReturnType<typeof persona>; vigente: boolean }>;
  urgencia?: { nivel: 'URGENTE' | 'PRIORITARIO' | 'NORMAL'; horas: number } | null;
  vencida?: boolean;
  /** Entrada vigente de cada etapa (por defecto, la primera). */
  entradas?: Record<number, string>;
}

export function pedidoDetalle(opciones: OpcionesPedido = {}) {
  const actual = opciones.etapaActual ?? 1;
  return {
    id: PEDIDO_ID,
    numero: NUMERO,
    nombre: 'Pedido ficticio de lámparas',
    pais: 'EC',
    tipoFlujo: 'A',
    etapaActual: actual,
    nombreEtapaActual: ETAPAS_A[actual - 1][0],
    responsableActual: null,
    venceEn: actual > 1 ? '2026-10-20T16:00:00-05:00' : null,
    vencida: opciones.vencida ?? false,
    motivoRetraso: null,
    urgencia: opciones.urgencia
      ? { ...opciones.urgencia, activadaEn: '2026-10-15T10:00:00-05:00', venceEn: '2026-10-15T14:00:00-05:00' }
      : null,
    finalizado: false,
    descripcion: 'Descripción ficticia',
    incluyeProductosNuevos: false,
    creadoPor: ADMIN_PERSONA,
    creadoEn: '2026-10-14T09:00:00-05:00',
    versionFlujo: 1,
    bodega: null,
    demoraAduana: null,
    lineaDeTiempo: ETAPAS_A.map(([nombre, rol], indice) => {
      const numero = indice + 1;
      const estado = numero < actual ? 'COMPLETADA' : numero === actual ? 'ACTUAL' : 'PENDIENTE';
      const asignacion = opciones.asignaciones?.[numero];
      return {
        numero,
        nombre,
        estado,
        rolResponsable: rol,
        estadoPlazo: numero === 1 || numero === 11 ? 'SIN_PLAZO' : numero === 5 ? 'PENDIENTE' : 'DEFINIDO',
        asignadoA: asignacion?.persona ?? null,
        asignacionVigente: asignacion ? asignacion.vigente : null,
        entradaId: numero <= actual ? (opciones.entradas?.[numero] ?? entradaDe(numero)) : null,
        entrada: numero <= actual ? '2026-10-15T09:00:00-05:00' : null,
        salida: numero < actual ? '2026-10-15T10:00:00-05:00' : null,
        venceEn: numero === actual && numero > 1 ? '2026-10-20T16:00:00-05:00' : null,
        reentradas: 0,
        resumen: opciones.resumenes?.[numero] ?? null,
      };
    }),
    accionesDisponibles: opciones.acciones ?? [],
  };
}

export type Detalle = ReturnType<typeof pedidoDetalle>;

export function resumenDe(detalle: Detalle) {
  return {
    id: detalle.id,
    numero: detalle.numero,
    nombre: detalle.nombre,
    pais: detalle.pais,
    tipoFlujo: detalle.tipoFlujo,
    etapaActual: detalle.etapaActual,
    nombreEtapaActual: detalle.nombreEtapaActual,
    responsableActual: detalle.responsableActual,
    venceEn: detalle.venceEn,
    vencida: detalle.vencida,
    motivoRetraso: null,
    urgencia: detalle.urgencia,
    finalizado: false,
  };
}

export function pagina(elementos: unknown[], total = elementos.length, numero = 0, tamano = 20) {
  return { elementos, pagina: numero, tamano, total };
}

export function adjunto(id: string, etapa: number, entradaId: string, tipoDocumento: string, nombre?: string) {
  return {
    id,
    pedidoId: PEDIDO_ID,
    etapa,
    entradaId,
    eventoId: null,
    nombre: nombre ?? `${tipoDocumento} ficticio.pdf`,
    tipoDocumento,
    tamanoBytes: 2048,
    subidoPor: ADMIN_PERSONA,
    subidoEn: '2026-10-15T09:30:00-05:00',
    estadoCopia: 'PENDIENTE',
  };
}

export function evento(id: string, tipo: string, etapa: number, extra: Record<string, unknown> = {}) {
  return {
    id,
    pedidoId: PEDIDO_ID,
    numeroPedido: NUMERO,
    etapa,
    tipo,
    usuario: ADMIN_PERSONA,
    rolIntervencion: 'admin',
    detalle: {
      etapaDestino: null,
      motivoDevolucion: null,
      urgenciaTerminada: null,
      asignadoAnterior: null,
      asignadoA: null,
    },
    fecha: '2026-10-15T09:00:00-05:00',
    observacion: null,
    nivelUrgencia: null,
    adjuntoIds: [],
    ...extra,
  };
}

/** Definición del flujo A v1: nombres, roles y revisión (A3 revisa A2, P-17). */
export const definicionFlujoA = {
  pais: 'EC',
  tipoFlujo: 'A',
  version: 1,
  etapas: ETAPAS_A.map(([nombre, rol], indice) => ({
    numero: indice + 1,
    nombre,
    rolResponsable: rol,
    estadoPlazo: 'DEFINIDO',
    revisaEtapa: indice + 1 === 3 ? 2 : null,
    plazo: null,
    acciones: [],
    automatica: indice + 1 === 11,
  })),
};

export interface Llamada {
  metodo: string;
  ruta: string;
  cuerpo?: unknown;
}

/**
 * API de un pedido con estado. `estado.detalle` es lo que devuelve GET /pedidos/{id}; cada prueba agrega las
 * escrituras que necesita. `llamadas` registra cada petición para comprobar qué se volvió a leer o a enviar.
 */
export function apiPedido(
  inicial: Detalle,
  opciones: { adjuntos?: Record<number, unknown[]>; historial?: unknown[] } = {},
) {
  const estado = {
    detalle: inicial,
    adjuntos: opciones.adjuntos ?? ({} as Record<number, unknown[]>),
    historial: opciones.historial ?? [evento('0193f0a0-0000-7000-a000-000000000001', 'PEDIDO_CREADO', 1)],
  };
  const llamadas: Llamada[] = [];
  const registrar = (metodo: string, ruta: string, cuerpo?: unknown) => llamadas.push({ metodo, ruta, cuerpo });
  const cuenta = (metodo: string, sufijo: string) =>
    llamadas.filter((l) => l.metodo === metodo && l.ruta.endsWith(sufijo)).length;

  const base = `*/api/v1/pedidos/${PEDIDO_ID}`;
  const manejadores = [
    http.get(base, () => {
      registrar('GET', '/detalle');
      return HttpResponse.json(estado.detalle);
    }),
    http.get(`${base}/historial`, () => {
      registrar('GET', '/historial');
      return HttpResponse.json(estado.historial);
    }),
    http.get(`${base}/etapas/:etapa/archivos`, ({ params }) => {
      registrar('GET', `/etapas/${String(params.etapa)}/archivos`);
      return HttpResponse.json(estado.adjuntos[Number(params.etapa)] ?? []);
    }),
    http.get('*/api/v1/flujos/EC/A', () => HttpResponse.json(definicionFlujoA)),
  ];
  return { estado, llamadas, manejadores, cuenta, registrar, base };
}
