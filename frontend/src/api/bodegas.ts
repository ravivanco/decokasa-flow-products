import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { useSesion } from '@/auth/SesionProvider';
import { type CodigoPais, segmentoPais, usePaisActivo } from '@/pais/PaisProvider';
import { esquemaBodega } from './esquemas';

export const clavesBodegas = {
  todas: (sesion: number) => ['sesion', sesion, 'bodegas'] as const,
  lista: (sesion: number, pais: CodigoPais, activa?: boolean) =>
    ['sesion', sesion, 'bodegas', segmentoPais(pais), { activa: activa ?? null }] as const,
};

/**
 * Bodegas del país activo (paso 7.3; contrato 0.17.0). El servidor solo devuelve las de los países asignados y responde
 * 403 si el país pedido ya no lo está.
 */
export function useBodegas(activa?: boolean, habilitada = true) {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  return useQuery({
    queryKey: clavesBodegas.lista(sesionId, pais, activa),
    queryFn: ({ signal }) => pedir(`/bodegas?pais=${pais}${activa === undefined ? '' : `&activa=${activa}`}`, {
      esquema: z.array(esquemaBodega), signal,
    }),
    enabled: habilitada,
    staleTime: 10_000,
  });
}

// La ciudad puede faltar solo al editar una bodega con la ciudad pendiente (no se envía y no cambia).
type DatosBodega = { nombre: string; ciudad?: string; activa: boolean } & (
  { id?: undefined; version?: undefined } | { id: string; version: number }
);

export function useGuardarBodega() {
  const { pedir, sesionId } = useSesion();
  const pais = usePaisActivo();
  const consultas = useQueryClient();
  return useMutation({
    retry: false,
    mutationFn: (datos: DatosBodega) => datos.id
      ? pedir(`/bodegas/${encodeURIComponent(datos.id)}`, {
          metodo: 'PATCH', cuerpo: { nombre: datos.nombre, ciudad: datos.ciudad, activa: datos.activa, version: datos.version },
          esquema: esquemaBodega,
        })
      // Paso 7.3: el alta es del país activo; el servidor exige Logística o admin con ese país.
      : pedir('/bodegas', { metodo: 'POST', cuerpo: { nombre: datos.nombre, ciudad: datos.ciudad, activa: datos.activa, pais }, esquema: esquemaBodega }),
    // Un nombre/estado modificado afecta también a pedidos que ya tienen esa bodega. Incluye conflictos y 403.
    onSettled: () => Promise.all([
      consultas.invalidateQueries({ queryKey: clavesBodegas.todas(sesionId) }),
      consultas.invalidateQueries({ queryKey: ['sesion', sesionId, 'pedidos'] }),
    ]),
  });
}
