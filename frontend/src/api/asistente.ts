import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { z } from 'zod';
import { useSesion } from '@/auth/SesionProvider';
import { type CodigoPais, segmentoPais, usePaisActivo } from '@/pais/PaisProvider';

// Asistente del piloto (contrato 0.19.0-borrador; ADR-0020 a ADR-0023; pasos 7.4 y 7.5): abrir una conversación,
// listar las propias, leer una y preguntar. El servidor decide permisos, límites y presupuesto; aquí no se reintenta
// nada que pueda gastar.
//
// Aislamiento entre cuentas y países (paso 7.3):
//  - Toda clave lleva la sesión y el segmento `pais:XX`. Al cambiar de país, PaisProvider cancela y borra las consultas
//    del país anterior, así que una respuesta tardía no llega a la pantalla del país nuevo.
//  - Al cambiar de cuenta, la sesión cambia de generación y `pedir` descarta las respuestas de la anterior.
//  - El resultado de una mutación solo se guarda si sigue la misma sesión y el mismo país que al enviarla.
//  - Nada se guarda en localStorage ni en sessionStorage (F7-D5): solo la caché en memoria.

export const esquemaReferenciaAsistente = z.object({
  numeroPedido: z.string(),
  etapa: z.number().int().nullish(),
  archivoId: z.string().nullish(),
  nombreArchivo: z.string().nullish(),
  pagina: z.number().int().min(1).nullish(),
  textoCitado: z.string().nullish(),
});
export type ReferenciaAsistente = z.infer<typeof esquemaReferenciaAsistente>;

export const esquemaDocumentoExcluido = z.object({
  archivoId: z.string(),
  nombreArchivo: z.string(),
  motivo: z.enum(['FORMATO_NO_ADMITIDO', 'CONTEXTO_EXCEDIDO', 'NO_AUTORIZADO_PILOTO']),
});
export type DocumentoExcluidoAsistente = z.infer<typeof esquemaDocumentoExcluido>;

export const esquemaConversacionAsistente = z.object({
  id: z.string(),
  pedidoId: z.string(),
  numeroPedido: z.string(),
  creadaEn: z.string(),
  expiraEn: z.string(),
  preguntasRestantes: z.number().int().min(0),
});
export type ConversacionAsistente = z.infer<typeof esquemaConversacionAsistente>;

// Un mensaje retenido (permiso de descarga perdido) llega sin texto, evidencia, referencias ni documentos excluidos.
export const esquemaMensajeAsistente = z.object({
  orden: z.number().int().min(1),
  autor: z.enum(['USUARIO', 'ASISTENTE']),
  creadoEn: z.string(),
  retenido: z.boolean(),
  texto: z.string().optional(),
  evidenciaSuficiente: z.boolean().optional(),
  referencias: z.array(esquemaReferenciaAsistente).optional(),
  documentosExcluidos: z.array(esquemaDocumentoExcluido).optional(),
});
export type MensajeAsistente = z.infer<typeof esquemaMensajeAsistente>;

export const esquemaConversacionAsistenteDetalle = esquemaConversacionAsistente.extend({
  mensajes: z.array(esquemaMensajeAsistente),
});
export type ConversacionAsistenteDetalle = z.infer<typeof esquemaConversacionAsistenteDetalle>;

const esquemaPaginaConversaciones = z.object({
  elementos: z.array(esquemaConversacionAsistente),
  pagina: z.number().int(),
  tamano: z.number().int(),
  total: z.number().int(),
});

// F7-D4: roles que dan acceso. Solo decide si se muestra el acceso; el servidor vuelve a comprobarlo con los roles
// vigentes en la base.
export const ROLES_ASISTENTE = ['admin', 'asistente_compras', 'marketing', 'compras_china', 'logistica'] as const;
export function puedeUsarAsistente(roles: readonly string[]): boolean {
  return roles.some((rol) => (ROLES_ASISTENTE as readonly string[]).includes(rol));
}

export const esquemaRespuestaAsistente = z.object({
  texto: z.string(),
  evidenciaSuficiente: z.boolean(),
  referencias: z.array(esquemaReferenciaAsistente),
  documentosExcluidos: z.array(esquemaDocumentoExcluido),
  preguntasRestantes: z.number().int().min(0),
});
export type RespuestaAsistente = z.infer<typeof esquemaRespuestaAsistente>;

export const clavesAsistente = {
  todas: (sesion: number) => ['sesion', sesion, 'asistente'] as const,
  delPais: (sesion: number, pais: CodigoPais) => ['sesion', sesion, 'asistente', segmentoPais(pais)] as const,
  lista: (sesion: number, pais: CodigoPais, pedidoId?: string) =>
    ['sesion', sesion, 'asistente', segmentoPais(pais), 'conversaciones', { pedidoId: pedidoId ?? null }] as const,
  detalle: (sesion: number, pais: CodigoPais, conversacionId: string) =>
    ['sesion', sesion, 'asistente', segmentoPais(pais), 'conversacion', conversacionId] as const,
};

/** Conversaciones propias y vigentes del país activo; el servidor responde 403 si el país ya no está asignado. */
export function useConversacionesAsistente(pedidoId?: string, habilitada = true) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  const filtro = pedidoId ? `&pedidoId=${encodeURIComponent(pedidoId)}` : '';
  return useQuery({
    queryKey: clavesAsistente.lista(sesionId, pais, pedidoId),
    queryFn: ({ signal }) => pedir(`/asistente/conversaciones?pais=${pais}${filtro}`, {
      esquema: esquemaPaginaConversaciones, signal,
    }),
    enabled: habilitada,
    staleTime: 10_000,
  });
}

/**
 * Una conversación propia. El servidor vuelve a comprobar cuenta, rol, país y pedido: si ya no tiene acceso, 404 sin
 * contenido. Los mensajes retenidos se muestran como tales; la pantalla nunca intenta reconstruirlos.
 */
export function useConversacionAsistente(conversacionId: string | undefined) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesAsistente.detalle(sesionId, pais, conversacionId ?? ''),
    queryFn: ({ signal }) => pedir(`/asistente/conversaciones/${encodeURIComponent(conversacionId ?? '')}`, {
      esquema: esquemaConversacionAsistenteDetalle, signal,
    }),
    enabled: Boolean(conversacionId),
    // Sin reintentos ante 403/404: la falta de acceso no se corrige reintentando.
    retry: false,
  });
}

type Origen = { sesionId: number; pais: CodigoPais };

/** Abre una conversación sobre un pedido del país activo. No llama al modelo; con la IA desactivada responde 503. */
export function useAbrirConversacionAsistente() {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  const consultas = useQueryClient();
  const vigente = useRef<Origen>({ sesionId, pais });
  useEffect(() => {
    vigente.current = { sesionId, pais };
  }, [sesionId, pais]);
  return useMutation({
    retry: false,
    // La cuenta y el país se fijan al enviar, no al recibir la respuesta.
    onMutate: (): Origen => ({ ...vigente.current }),
    mutationFn: (pedidoId: string) =>
      pedir(`/pedidos/${encodeURIComponent(pedidoId)}/asistente/conversaciones`, {
        metodo: 'POST', esquema: esquemaConversacionAsistente,
      }),
    onSuccess: (conversacion, _pedidoId, origen) => {
      // Respuesta tardía de otra cuenta o de otro país: no se guarda ni se muestra.
      if (!origen || origen.sesionId !== vigente.current.sesionId || origen.pais !== vigente.current.pais) return;
      consultas.setQueryData(clavesAsistente.detalle(origen.sesionId, origen.pais, conversacion.id),
        { ...conversacion, mensajes: [] });
    },
    onSettled: (_conversacion, _error, _pedidoId, origen) => {
      if (!origen || origen.sesionId !== vigente.current.sesionId || origen.pais !== vigente.current.pais) return;
      return consultas.invalidateQueries({ queryKey: clavesAsistente.delPais(origen.sesionId, origen.pais) });
    },
  });
}

/**
 * Pregunta en una conversación (paso 7.5). Sin reintentos: cada intento puede consumir presupuesto. Al terminar se
 * vuelve a leer la conversación del país y la cuenta de origen, que trae la respuesta guardada con sus referencias
 * verificadas y sus documentos excluidos. Una respuesta que llega después de cambiar de cuenta o de país no se muestra.
 */
export function usePreguntarAsistente() {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  const consultas = useQueryClient();
  const vigente = useRef<Origen>({ sesionId, pais });
  useEffect(() => {
    vigente.current = { sesionId, pais };
  }, [sesionId, pais]);
  return useMutation({
    retry: false,
    onMutate: (): Origen => ({ ...vigente.current }),
    mutationFn: ({ conversacionId, pregunta }: { conversacionId: string; pregunta: string }) =>
      pedir(`/asistente/conversaciones/${encodeURIComponent(conversacionId)}/mensajes`, {
        metodo: 'POST', cuerpo: { pregunta }, esquema: esquemaRespuestaAsistente,
      }),
    onSettled: (_respuesta, _error, { conversacionId }, origen) => {
      if (!origen || origen.sesionId !== vigente.current.sesionId || origen.pais !== vigente.current.pais) return;
      return Promise.all([
        consultas.invalidateQueries({ queryKey: clavesAsistente.detalle(origen.sesionId, origen.pais, conversacionId) }),
        consultas.invalidateQueries({ queryKey: clavesAsistente.lista(origen.sesionId, origen.pais) }),
      ]);
    },
  });
}
