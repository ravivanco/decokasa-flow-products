import { useQuery } from '@tanstack/react-query';
import { z } from 'zod';
import { useSesion } from '@/auth/SesionProvider';

export const ESTADOS_CORREO = ['PENDIENTE', 'ENVIADA', 'FALLIDA'] as const;
export type EstadoCorreo = typeof ESTADOS_CORREO[number];
const esquemaFila = z.object({
  id: z.string().uuid(), pedidoId: z.string().uuid(), eventoId: z.string().uuid(),
  destinatario: z.string().nullable(), asunto: z.string(), estado: z.enum(ESTADOS_CORREO),
  creadaEn: z.string(), enviadaEn: z.string().nullable(), intentos: z.number().int().nonnegative(),
  ultimoError: z.string().nullable(), resultadoIncierto: z.boolean(), bloqueo: z.string().nullable(),
});
const esquemaPagina = z.object({ elementos: z.array(esquemaFila), pagina: z.number().int(),
  tamano: z.number().int(), total: z.number().int().nonnegative() });

export function useNotificaciones(estado: EstadoCorreo, pagina: number) {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: ['notificaciones', sesionId, { estado, pagina, tamano: 20 }],
    queryFn: ({ signal }) => pedir(`/notificaciones?estado=${estado}&pagina=${pagina}&tamano=20`, {
      esquema: esquemaPagina, signal,
    }),
    staleTime: 10_000, retry: false,
  });
}
