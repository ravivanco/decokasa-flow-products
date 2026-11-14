import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { useSesion } from '@/auth/SesionProvider';
import { usePaisActivo } from '@/pais/PaisProvider';
import { opcionesSeguimiento } from './actualizacion';
import { clavesPedidos, useRefrescarPedido } from './pedidos';

// BL, Packing Lists, envíos, checklist y alertas simuladas de un pedido. Todo lo decide el servidor; aquí no se calcula
// ningún permiso ni se lee el contenido de un archivo.

const esquemaPackingList = z.object({
  id: z.string(),
  version: z.number().int(),
  adjuntoId: z.string(),
  nombre: z.string(),
  subidoPor: z.string(),
  subidoEn: z.string(),
});
export type PackingList = z.infer<typeof esquemaPackingList>;

const esquemaPedidoDelBl = z.object({
  id: z.string(),
  numero: z.string(),
  vinculadoPor: z.string(),
  vinculadoEn: z.string(),
});

const esquemaBl = z.object({
  id: z.string(),
  codigo: z.string(),
  fechaSalida: z.string().nullable(),
  llegadaEstimada: z.string().nullable(),
  llegadaOrigen: z.enum(['AUTOMATICA', 'MANUAL', 'SIN_DATOS']),
  transitoDias: z.number().int().nullable(),
  version: z.number().int(),
  bloqueado: z.boolean(),
  estado: z.enum(['LISTO', 'SIN_PACKING_LIST', 'SIN_SALIDA']),
  packingList: esquemaPackingList.nullable(),
  versiones: z.array(esquemaPackingList),
  pedidos: z.array(esquemaPedidoDelBl),
  enAprobacionEn: z.array(z.string()),
  revisionPendiente: z.boolean(),
  aprobadoEnVersion: z.number().int().nullable(),
  importadora: z.enum(['DECOKASA', 'LATINOAMERICA']).nullish(),
});
export type Bl = z.infer<typeof esquemaBl>;

const esquemaEnvio = z.object({
  id: z.string(),
  numero: z.number().int(),
  estado: z.enum(['PENDIENTE', 'RECHAZADO', 'APROBADO']),
  enviadoPor: z.string(),
  enviadoEn: z.string(),
  cerradoEn: z.string().nullable(),
  filas: z.array(z.object({
    blId: z.string(),
    codigo: z.string(),
    fechaSalida: z.string(),
    llegadaEstimada: z.string().nullable(),
    llegadaOrigen: z.string(),
    packingListId: z.string(),
    version: z.number().int(),
    nombreArchivo: z.string(),
    pedidos: z.array(esquemaPedidoDelBl),
    importadora: z.string().nullish(),
  })),
  decisiones: z.array(z.object({
    decision: z.enum(['APROBADA', 'RECHAZADA']),
    usuario: z.string(),
    motivo: z.string().nullable(),
    blSenalados: z.array(z.string()),
    decididoEn: z.string(),
  })),
  revisiones: z.array(z.object({
    blId: z.string(),
    codigo: z.string(),
    version: z.number().int(),
    revisadoPor: z.string(),
    revisadoEn: z.string(),
  })),
});
export type Envio = z.infer<typeof esquemaEnvio>;

const esquemaVista = z.object({
  gestionaEmbarques: z.boolean(),
  apruebaEmbarques: z.boolean(),
  puedeEditar: z.boolean(),
  puedeRevisar: z.boolean(),
  transitoDias: z.number().int().nullable(),
  bls: z.array(esquemaBl),
  envios: z.array(esquemaEnvio),
});
export type VistaEmbarques = z.infer<typeof esquemaVista>;

const esquemaChecklist = z.object({
  aplica: z.boolean(),
  pendienteDefinicion: z.boolean(),
  total: z.number().int(),
  confirmados: z.number().int(),
  requisitos: z.array(z.object({
    tipoDocumento: z.string(),
    adjuntoId: z.string().nullable(),
    nombreArchivo: z.string().nullable(),
    // V35: la marca ✓ cumple el requisito; el archivo es opcional.
    marcado: z.boolean().default(false),
    marcadoPor: z.string().nullish(),
    marcadoEn: z.string().nullish(),
  })),
  entradaId: z.string().nullish(),
  puedeMarcar: z.boolean().default(false),
});
export type Checklist = z.infer<typeof esquemaChecklist>;

const esquemaAlertas = z.array(z.object({
  id: z.string(),
  destinatario: z.string(),
  texto: z.string(),
  creadoEn: z.string(),
}));

const ruta = (pedidoId: string) => `/pedidos/${encodeURIComponent(pedidoId)}`;

export function useEmbarques(pedidoId: string, habilitada = true) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesPedidos.embarques(sesionId, pais, pedidoId),
    queryFn: ({ signal }) => pedir(`${ruta(pedidoId)}/embarques`, { esquema: esquemaVista, signal }),
    enabled: habilitada,
    staleTime: 10_000,
    ...opcionesSeguimiento,
  });
}

export function useChecklist(pedidoId: string, habilitada: boolean) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesPedidos.checklist(sesionId, pais, pedidoId),
    queryFn: ({ signal }) => pedir(`${ruta(pedidoId)}/checklist-documentos`, { esquema: esquemaChecklist, signal }),
    enabled: habilitada,
  });
}

/** Marca ✓ o desmarca un requisito del checklist de la entrada actual (V35). */
export function useMarcarRequisito(pedidoId: string) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  const consultas = useQueryClient();
  const refrescar = useRefrescarPedido();
  const clave = clavesPedidos.checklist(sesionId, pais, pedidoId);
  return useMutation({
    mutationFn: (datos: { tipoDocumento: string; marcado: boolean; entradaEsperadaId: string }) =>
      pedir(`${ruta(pedidoId)}/checklist-documentos/marcas`, { metodo: 'POST', cuerpo: datos, esquema: esquemaChecklist }),
    onSuccess: (checklist: Checklist) => {
      consultas.setQueryData(clave, checklist);
      // Las acciones disponibles cambian: lo marcado cuenta como documento presente.
      refrescar(pedidoId);
    },
    onError: () => { void consultas.invalidateQueries({ queryKey: clave }); },
  });
}

export function useAlertasSimuladas(pedidoId: string) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesPedidos.alertasSimuladas(sesionId, pais, pedidoId),
    queryFn: ({ signal }) => pedir(`${ruta(pedidoId)}/alertas-simuladas`, { esquema: esquemaAlertas, signal }),
    staleTime: 10_000,
    ...opcionesSeguimiento,
  });
}

/** Mutaciones de un BL: guardan la vista que devuelve el servidor y refrescan lo que depende de ella. */
export function useMutacionesEmbarques(pedidoId: string) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  const consultas = useQueryClient();
  const refrescar = useRefrescarPedido();
  const opciones = {
    onSuccess: (vista: VistaEmbarques) => {
      consultas.setQueryData(clavesPedidos.embarques(sesionId, pais, pedidoId), vista);
      refrescar(pedidoId);
    },
  };
  const llamar = (camino: string, metodo: 'POST' | 'PATCH' | 'DELETE', cuerpo?: unknown) =>
    pedir(`${ruta(pedidoId)}/embarques${camino}`, { metodo, cuerpo, esquema: esquemaVista });
  return {
    crear: useMutation({
      mutationFn: (datos: { codigo: string; fechaSalida: string | null; llegadaEstimada: string | null;
        importadora: string | null }) =>
        llamar('', 'POST', datos),
      ...opciones,
    }),
    actualizar: useMutation({
      mutationFn: ({ blId, ...datos }: { blId: string; version: number; fechaSalida: string | null;
        llegadaEstimada: string | null; llegadaOrigen: string | null; importadora: string | null }) =>
        llamar(`/${blId}`, 'PATCH', datos),
      ...opciones,
    }),
    registrarPackingList: useMutation({
      mutationFn: ({ blId, adjuntoId }: { blId: string; adjuntoId: string }) =>
        llamar(`/${blId}/packing-list`, 'POST', { adjuntoId }),
      ...opciones,
    }),
    vincular: useMutation({
      mutationFn: (codigo: string) => llamar('/vinculos', 'POST', { codigo }),
      ...opciones,
    }),
    anadirPedido: useMutation({
      mutationFn: ({ blId, numero }: { blId: string; numero: string }) => llamar(`/${blId}/pedidos`, 'POST', { numero }),
      ...opciones,
    }),
    desvincular: useMutation({
      mutationFn: (blId: string) => llamar(`/${blId}/vinculo`, 'DELETE'),
      ...opciones,
    }),
    marcarRevisada: useMutation({
      mutationFn: (blId: string) => llamar(`/${blId}/revision`, 'POST'),
      ...opciones,
    }),
  };
}

/** Importadoras que se eligen en Gestión de embarques (pedido del cliente, 2026-11-14). */
export const IMPORTADORAS = [
  { valor: 'DECOKASA', nombre: 'DECOKASA' },
  { valor: 'LATINOAMERICA', nombre: 'Latinoamérica' },
] as const;
export const nombreImportadora = (valor: string | null | undefined) =>
  IMPORTADORAS.find((importadora) => importadora.valor === valor)?.nombre ?? 'Sin elegir';

/** Tipo visible según la extensión; lo demás se descarga. */
export function tipoVisible(nombre: string): 'application/pdf' | 'image/png' | 'image/jpeg' | null {
  const minusculas = nombre.toLowerCase();
  if (minusculas.endsWith('.pdf')) return 'application/pdf';
  if (minusculas.endsWith('.png')) return 'image/png';
  if (minusculas.endsWith('.jpg') || minusculas.endsWith('.jpeg')) return 'image/jpeg';
  return null;
}

export function usePackingListContenido(pedidoId: string) {
  const { pedir } = useSesion();
  return useMutation({
    mutationFn: async ({ blId, packingListId, nombre }: { blId: string; packingListId: string; nombre: string }) => {
      const contenido = await pedir<Blob>(`${ruta(pedidoId)}/embarques/${blId}/packing-lists/${packingListId}/contenido`,
        { formato: 'blob' });
      const tipo = tipoVisible(nombre);
      return { url: URL.createObjectURL(tipo ? new Blob([contenido], { type: tipo }) : contenido), tipo, nombre };
    },
  });
}
