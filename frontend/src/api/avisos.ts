import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { useSesion } from '@/auth/SesionProvider';
import { opcionesSeguimiento } from './actualizacion';

// Avisos de etapa (ADR-0032): la admin recibe uno por cada transición y la persona que recibe el trabajo, el suyo. Los
// guarda el servidor en la misma transacción que la acción; aquí no se decide quién recibe qué. La campana y Mi bandeja
// usan esta misma consulta y la misma marca de lectura. Se actualizan con la consulta periódica (ADR-0014).

export const clavesAvisos = {
  todos: (sesion: number) => ['sesion', sesion, 'avisos'] as const,
};

export const esquemaAviso = z.object({
  eventoId: z.string(),
  pedidoId: z.string(),
  numero: z.string(),
  pais: z.string(),
  tipoFlujo: z.string(),
  tipo: z.enum(['CREACION', 'LLEGADA', 'DEVOLUCION', 'REASIGNACION', 'CIERRE']),
  etapaOrigen: z.number().int().nullable(),
  nombreOrigen: z.string().nullable(),
  etapaDestino: z.number().int(),
  nombreDestino: z.string(),
  actor: z.string().nullable(),
  motivo: z.string().nullable(),
  ocurridoEn: z.string(),
  comoAdmin: z.boolean(),
  comoResponsable: z.boolean(),
  leido: z.boolean(),
});
export type Aviso = z.infer<typeof esquemaAviso>;

const esquemaAvisos = z.object({
  avisos: z.array(esquemaAviso),
  noLeidos: z.number().int(),
  leidasHasta: z.string(),
});

export function useAvisos() {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: clavesAvisos.todos(sesionId),
    queryFn: ({ signal }) => pedir('/avisos', { esquema: esquemaAvisos, signal }),
    staleTime: 30_000,
    ...opcionesSeguimiento,
  });
}

/** Marca como leídos mis avisos hasta la hora del servidor. */
export function useMarcarAvisosLeidos() {
  const { pedir, sesionId } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: () =>
      pedir('/avisos/leidos', { metodo: 'POST', esquema: z.object({ leidasHasta: z.string() }) }),
    onSettled: () => consultas.invalidateQueries({ queryKey: clavesAvisos.todos(sesionId) }),
  });
}
