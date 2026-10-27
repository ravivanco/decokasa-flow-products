import { z } from 'zod';

/**
 * Catálogo real (F4-D1): el PATCH devuelve la versión que debe conservar la siguiente edición. Contrato 0.17.0 (fase 7):
 * cada bodega tiene país, y la ciudad es nula mientras está pendiente (Bodega 1 de Colombia).
 */
export const esquemaBodega = z.object({
  id: z.string(), nombre: z.string(), ciudad: z.string().nullable(), activa: z.boolean(),
  version: z.number().int().nonnegative(), pais: z.enum(['EC', 'CO']).optional(),
});
export type Bodega = z.infer<typeof esquemaBodega>;

/** Texto de la ciudad para mostrar; nunca se usa como valor de un formulario. */
export function ciudadDeBodega(bodega: Pick<Bodega, 'ciudad'>): string {
  return bodega.ciudad ?? 'Ciudad pendiente';
}

// Esquemas de las respuestas de la API, iguales al contrato (documentos/api/openapi.yaml 0.5.0). El cliente valida
// cada respuesta con ellos: si el backend cambia la forma sin cambiar el contrato, falla aquí y no en la pantalla.

export const esquemaUsuario = z.object({
  id: z.string(),
  nombre: z.string(),
  correo: z.string(),
  usuario: z.string(),
  telefono: z.string(),
  roles: z.array(z.string()),
  paises: z.array(z.string()),
  activo: z.boolean(),
  // Versión administrativa (D10): se envía tal cual en el PATCH para no sobrescribir cambios de otra persona.
  version: z.number().int().nonnegative(),
});
export type Usuario = z.infer<typeof esquemaUsuario>;

export const esquemaUsuarioActual = esquemaUsuario.extend({
  permisos: z.array(z.string()),
});
export type UsuarioActual = z.infer<typeof esquemaUsuarioActual>;

export const esquemaRespuestaSesion = z.object({
  tokenAcceso: z.string(),
  expiraEn: z.number().int(),
  usuario: esquemaUsuario,
});
export type RespuestaSesion = z.infer<typeof esquemaRespuestaSesion>;

export const esquemaRol = z.object({
  id: z.string(),
  codigo: z.string(),
  nombre: z.string(),
  permisos: z.array(z.string()),
});
export type Rol = z.infer<typeof esquemaRol>;

/** Problema RFC 9457 con el código estable del contrato. */
export const esquemaProblema = z.object({
  title: z.string().optional(),
  status: z.number().optional(),
  detail: z.string().optional(),
  codigo: z.string().optional(),
  errores: z.array(z.object({ campo: z.string(), mensaje: z.string() })).optional(),
  /** Requisitos que faltan, solo con REQUISITO_FALTANTE (422). */
  faltantes: z.array(z.string()).optional(),
});
