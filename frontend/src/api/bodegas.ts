import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { useSesion } from '@/auth/SesionProvider';
import { esquemaBodega } from './esquemas';

export const clavesBodegas = {
  todas: (sesion: number) => ['sesion', sesion, 'bodegas'] as const,
  lista: (sesion: number, activa?: boolean) => ['sesion', sesion, 'bodegas', { activa: activa ?? null }] as const,
};

/** La lectura admite cualquier sesión según OpenAPI; mantenimiento y selección se ofrecen donde corresponde. */
export function useBodegas(activa?: boolean, habilitada = true) {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: clavesBodegas.lista(sesionId, activa),
    queryFn: ({ signal }) => pedir(`/bodegas${activa === undefined ? '' : `?activa=${activa}`}`, {
      esquema: z.array(esquemaBodega), signal,
    }),
    enabled: habilitada,
    staleTime: 10_000,
  });
}

type DatosBodega = { nombre: string; ciudad: string; activa: boolean } & (
  { id?: undefined; version?: undefined } | { id: string; version: number }
);

export function useGuardarBodega() {
  const { pedir, sesionId } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    retry: false,
    mutationFn: (datos: DatosBodega) => datos.id
      ? pedir(`/bodegas/${encodeURIComponent(datos.id)}`, {
          metodo: 'PATCH', cuerpo: { nombre: datos.nombre, ciudad: datos.ciudad, activa: datos.activa, version: datos.version },
          esquema: esquemaBodega,
        })
      : pedir('/bodegas', { metodo: 'POST', cuerpo: { nombre: datos.nombre, ciudad: datos.ciudad, activa: datos.activa }, esquema: esquemaBodega }),
    // Un nombre/estado modificado afecta también a pedidos que ya tienen esa bodega. Incluye conflictos y 403.
    onSettled: () => Promise.all([
      consultas.invalidateQueries({ queryKey: clavesBodegas.todas(sesionId) }),
      consultas.invalidateQueries({ queryKey: ['sesion', sesionId, 'pedidos'] }),
    ]),
  });
}
