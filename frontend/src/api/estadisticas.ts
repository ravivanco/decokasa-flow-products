import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { z } from 'zod';
import { useSesion } from '@/auth/SesionProvider';
import { opcionesSeguimiento } from './actualizacion';

// Inicio común (ADR-0029). Los agregados los calcula el servidor sobre todos los pedidos que la persona puede
// consultar (no sobre una página); aquí no se filtra nada sensible. La caché se separa por sesión y por filtros, y las
// operaciones sobre pedidos invalidan estas claves (useCrearPedido, useRefrescarPedido).

export const clavesEstadisticas = {
  todas: (sesion: number) => ['sesion', sesion, 'estadisticas'] as const,
  alertas: (sesion: number) => ['sesion', sesion, 'alertas'] as const,
};

const numeroONulo = z.number().nullable();

export const esquemaEstadisticas = z.object({
  periodo: z.object({ anio: z.number().int(), desde: z.string(), hasta: z.string() }),
  paises: z.array(z.string()),
  tarjetas: z.object({
    creados: z.number().int(),
    enCurso: z.number().int(),
    finalizados: z.number().int(),
    conRetraso: z.number().int(),
    medianaResolucionSegundos: numeroONulo,
    resolucionObservaciones: z.number().int(),
  }),
  porAnio: z.array(z.object({ anio: z.number().int(), pedidos: z.number().int() })),
  porMes: z.array(z.object({ mes: z.number().int(), pedidos: z.number().int().nullable() })),
  tiempoPorEtapa: z.array(
    z.object({
      tipoFlujo: z.string(),
      pais: z.string(),
      etapa: z.number().int(),
      nombreEtapa: z.string(),
      medianaSegundos: numeroONulo,
      promedioSegundos: numeroONulo,
      terminadas: z.number().int(),
      abiertas: z.number().int(),
    }),
  ),
  tiempoPorPedido: z.array(
    z.object({
      pedidoId: z.string(),
      numero: z.string(),
      tipoFlujo: z.string(),
      pais: z.string(),
      creadoEn: z.string(),
      finalizadoEn: z.string().nullable(),
      finalizado: z.boolean(),
      segundos: z.number(),
    }),
  ),
  tiempoPorPedidoTotal: z.number().int(),
  estado: z.object({
    enCurso: z.number().int(),
    finalizados: z.number().int(),
    enCursoConRetraso: z.number().int(),
    enCursoConUrgencia: z.number().int(),
    enCursoConDemora: z.number().int(),
  }),
  retrasosPorEtapa: z.array(
    z.object({
      tipoFlujo: z.string(),
      etapa: z.number().int(),
      nombreEtapa: z.string(),
      pedidos: z.number().int(),
      etapaVencida: z.number().int(),
      urgenciaVencida: z.number().int(),
    }),
  ),
  calculadoEn: z.string(),
});
export type Estadisticas = z.infer<typeof esquemaEstadisticas>;

export interface FiltrosEstadisticas {
  anio: number;
  pais?: string;
  tipoFlujo?: 'A' | 'B';
  desde?: string;
  hasta?: string;
}

export function useEstadisticas(filtros: FiltrosEstadisticas, habilitada = true) {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: [...clavesEstadisticas.todas(sesionId), filtros] as const,
    enabled: habilitada,
    queryFn: ({ signal }) => {
      const parametros = new URLSearchParams({ anio: String(filtros.anio) });
      if (filtros.pais) parametros.set('pais', filtros.pais);
      if (filtros.tipoFlujo) parametros.set('tipoFlujo', filtros.tipoFlujo);
      if (filtros.desde) parametros.set('desde', filtros.desde);
      if (filtros.hasta) parametros.set('hasta', filtros.hasta);
      return pedir(`/estadisticas/pedidos?${parametros}`, { esquema: esquemaEstadisticas, signal });
    },
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    ...opcionesSeguimiento,
  });
}

export const esquemaAlerta = z.object({
  pedidoId: z.string(),
  numero: z.string(),
  nombrePedido: z.string(),
  pais: z.string(),
  tipoFlujo: z.string(),
  etapa: z.number().int(),
  nombreEtapa: z.string(),
  tipo: z.enum(['URGENCIA_VENCIDA', 'ETAPA_VENCIDA', 'DEMORA_REPORTADA']),
  vence: z.string().nullable(),
  nuevaFechaLlegada: z.string().nullable(),
  motivo: z.string().nullable(),
  asignadoA: z.string().nullable(),
});
export type Alerta = z.infer<typeof esquemaAlerta>;

/** Alertas activas: de mis responsabilidades o, solo el admin, de todo su ámbito. */
export function useAlertas(ambito: 'MIAS' | 'TODAS') {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: [...clavesEstadisticas.alertas(sesionId), ambito] as const,
    queryFn: ({ signal }) => pedir(`/alertas?ambito=${ambito}`, { esquema: z.array(esquemaAlerta), signal }),
    staleTime: 30_000,
    ...opcionesSeguimiento,
  });
}
