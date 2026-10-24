import { useQuery } from '@tanstack/react-query';
import { z } from 'zod';
import { useSesion } from '@/auth/SesionProvider';
import { segmentoPais, usePaisActivo } from '@/pais/PaisProvider';

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
  // Paso 7.3: solo correos de pedidos del país activo (el servidor lo vuelve a filtrar con los países asignados).
  const pais = usePaisActivo();
  return useQuery({
    queryKey: ['notificaciones', sesionId, segmentoPais(pais), { estado, pagina, tamano: 20 }],
    queryFn: ({ signal }) => pedir(`/notificaciones?pais=${pais}&estado=${estado}&pagina=${pagina}&tamano=20`, {
      esquema: esquemaPagina, signal,
    }),
    staleTime: 10_000, retry: false,
  });
}
