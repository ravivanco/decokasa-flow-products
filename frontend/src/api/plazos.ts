import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { useSesion } from '@/auth/SesionProvider';
import { esErrorApi } from './cliente';

// Configuración de plazos (contrato GET/PUT /configuracion/plazos, 0.11.0; F5-D6). Solo la meta de días: los
// feriados siguen pendientes del cliente. La meta es una propuesta configurable pendiente del cliente; cambiarla no la
// vuelve un objetivo aprobado.

export const esquemaConfiguracionPlazos = z.object({
  metaDias: z.number().int().min(1),
  origenMeta: z.enum(['PROPUESTA', 'ADMIN']),
  metaPendienteDelCliente: z.boolean(),
  feriados: z.array(z.unknown()),
  actualizadoEn: z.string().nullish(),
  version: z.number().int().nonnegative(),
});
export type ConfiguracionPlazos = z.infer<typeof esquemaConfiguracionPlazos>;

export const clavesPlazos = {
  configuracion: (sesion: number) => ['sesion', sesion, 'plazos', 'configuracion'] as const,
};

export function useConfiguracionPlazos() {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: clavesPlazos.configuracion(sesionId),
    queryFn: ({ signal }) => pedir('/configuracion/plazos', { esquema: esquemaConfiguracionPlazos, signal }),
  });
}

/**
 * Cambia la meta con la versión leída (D10). Ante 409 VERSION_DESACTUALIZADA no reenvía nada: relee la configuración
 * para mostrar el valor vigente y la persona decide. Al guardar, los cronogramas se vuelven a pedir (usan la meta).
 */
export function useActualizarMeta() {
  const { pedir, sesionId } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: (cuerpo: { metaDias: number; version: number }) =>
      pedir('/configuracion/plazos', { metodo: 'PUT', cuerpo, esquema: esquemaConfiguracionPlazos }),
    onSuccess: (configuracion) => {
      consultas.setQueryData(clavesPlazos.configuracion(sesionId), configuracion);
      void consultas.invalidateQueries({ queryKey: ['sesion', sesionId, 'pedidos', 'cronograma'] });
    },
    onError: (error) => {
      if (esErrorApi(error, 'VERSION_DESACTUALIZADA')) {
        void consultas.invalidateQueries({ queryKey: clavesPlazos.configuracion(sesionId) });
      }
    },
  });
}
