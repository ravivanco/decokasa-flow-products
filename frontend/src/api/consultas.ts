import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { useSesion } from '@/auth/SesionProvider';
import { esquemaRol, esquemaUsuario, type Rol, type Usuario } from './esquemas';

// Consultas y mutaciones de /usuarios y /roles (TanStack Query). Las claves salen de una fábrica para invalidar con
// precisión. Las mutaciones nunca se reintentan solas: un 409 (DUPLICADO o VERSION_DESACTUALIZADA) lo resuelve la
// persona, sin sobrescribir ni reenviar cambios.

export const claves = {
  usuarios: (sesion: number) => ['sesion', sesion, 'usuarios'] as const,
  roles: (sesion: number) => ['sesion', sesion, 'roles'] as const,
};

/**
 * Carpeta de Drive de cada usuario (ADR-0026). CREADA solo con la confirmación de Drive guardada en el servidor; con
 * Google apagado queda PENDIENTE. No trae IDs ni enlaces de Drive.
 */
export interface NuevoUsuario {
  nombre: string;
  correo: string;
  usuario: string;
  telefono: string;
  contrasenaInicial: string;
  roles: string[];
  paises: string[];
}

/** PATCH /usuarios/{id}: la versión leída y solo los campos que cambian (contrato 0.5.0, D10). */
export type CambiosUsuario = { id: string; version: number } & Partial<
  Pick<Usuario, 'nombre' | 'correo' | 'usuario' | 'telefono' | 'roles' | 'paises' | 'activo'>
>;

export interface DatosRol {
  codigo: string;
  nombre: string;
  permisos: string[];
}

export function useUsuarios() {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: claves.usuarios(sesionId),
    queryFn: ({ signal }) => pedir('/usuarios', { esquema: z.array(esquemaUsuario), signal }),
  });
}

export function useRoles() {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: claves.roles(sesionId),
    queryFn: ({ signal }) => pedir('/roles', { esquema: z.array(esquemaRol), signal }),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCrearUsuario() {
  const { pedir, sesionId } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: (datos: NuevoUsuario) =>
      pedir('/usuarios', { metodo: 'POST', cuerpo: datos, esquema: esquemaUsuario }),
    // También refresca las carpetas (prefijo común): el alta programa la carpeta de Drive del usuario nuevo.
    onSuccess: () => consultas.invalidateQueries({ queryKey: claves.usuarios(sesionId) }),
  });
}

export function useActualizarUsuario() {
  const { pedir, sesionId } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: ({ id, ...cuerpo }: CambiosUsuario) =>
      pedir(`/usuarios/${encodeURIComponent(id)}`, { metodo: 'PATCH', cuerpo, esquema: esquemaUsuario }),
    // También tras un error: si fue 409 VERSION_DESACTUALIZADA, la lista se trae de nuevo para ver lo vigente.
    onSettled: () => consultas.invalidateQueries({ queryKey: claves.usuarios(sesionId) }),
  });
}

export function useGuardarRol() {
  const { pedir, sesionId } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: ({ id, ...cuerpo }: DatosRol & { id?: string }): Promise<Rol> =>
      id
        ? pedir(`/roles/${encodeURIComponent(id)}`, { metodo: 'PUT', cuerpo, esquema: esquemaRol })
        : pedir('/roles', { metodo: 'POST', cuerpo, esquema: esquemaRol }),
    onSuccess: () => consultas.invalidateQueries({ queryKey: claves.roles(sesionId) }),
  });
}
