import { CODIGO_SIN_CONEXION, esErrorApi } from './cliente';

// Textos para los errores de la API que se repiten en todas las pantallas. Cada pantalla resuelve antes sus casos
// propios (credenciales, campos duplicados, versión desactualizada) y usa esto para el resto, con su texto general.
// Nunca se muestran detalles técnicos ni el `detail` del servidor fuera de los casos previstos.

export const MENSAJE_SIN_CONEXION = 'No hay conexión con el servidor. Revisa tu red e intenta de nuevo.';
export const MENSAJE_SIN_PERMISO =
  'Tu cuenta ya no tiene permiso para esta acción. Si te cambiaron los roles, cierra sesión y vuelve a entrar.';

/** El mensaje para mostrar ante un error de la API, o `general` si no es un caso común. */
export function mensajeDeError(error: unknown, general: string): string {
  if (esErrorApi(error, CODIGO_SIN_CONEXION)) return MENSAJE_SIN_CONEXION;
  if (esErrorApi(error) && error.status === 403) return MENSAJE_SIN_PERMISO;
  return general;
}
