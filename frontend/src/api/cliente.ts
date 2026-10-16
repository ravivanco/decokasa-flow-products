import type { z } from 'zod';
import { esquemaProblema } from './esquemas';

/**
 * Base de la API. Sin VITE_API_URL (lo normal) es `/api/v1` en el mismo origen: en desarrollo la envía el proxy de
 * Vite y en Docker la envía nginx, y así se cumple la condición de D3b (frontend y API en el mismo sitio, cookie
 * `SameSite=Strict`, comprobación de `Origin`). VITE_API_URL solo se define si la API está en otra dirección, como
 * base completa incluida la versión (por ejemplo `https://api.ejemplo.test/api/v1`); entonces la API debe estar en el
 * mismo sitio y su CORS_ORIGENES debe incluir el origen del frontend. Nunca lleva secretos: es código público.
 */
export function baseApi(valor: string | undefined): string {
  const limpio = (valor ?? '').trim().replace(/\/+$/, '');
  return limpio || '/api/v1';
}

const BASE = baseApi(import.meta.env.VITE_API_URL);

/**
 * Un error de la API con el código estable del contrato (esquema Problema) y, si los hay, los campos afectados y los
 * requisitos que faltan (solo con REQUISITO_FALTANTE).
 */
export class ErrorApi extends Error {
  readonly status: number;
  readonly codigo: string;
  readonly errores: { campo: string; mensaje: string }[];
  readonly faltantes: string[];

  constructor(
    status: number,
    codigo: string,
    mensaje: string,
    errores: { campo: string; mensaje: string }[] = [],
    faltantes: string[] = [],
  ) {
    super(mensaje);
    this.name = 'ErrorApi';
    this.status = status;
    this.codigo = codigo;
    this.errores = errores;
    this.faltantes = faltantes;
  }
}

export interface OpcionesLlamada<T> {
  metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  cuerpo?: unknown;
  /** Token de acceso en memoria; va en `Authorization: Bearer`. */
  token?: string | null;
  /** Solo las rutas de sesión (/auth/*) envían y reciben la cookie de renovación (D3b). */
  credenciales?: boolean;
  /** Esquema del contrato con el que se valida la respuesta. */
  esquema?: z.ZodType<T>;
  /** Cancelación de consultas o de una sesión que ya no está vigente. */
  signal?: AbortSignal;
  /**
   * `blob` para descargar un archivo (respuesta binaria, sin esquema). Por defecto la respuesta es JSON. Los errores
   * siguen llegando como problema JSON en los dos casos.
   */
  formato?: 'json' | 'blob';
}

function url(ruta: string): string {
  const completa = `${BASE}${ruta}`;
  // En el navegador una ruta relativa ya es del mismo origen; se resuelve igual para que también funcione en pruebas.
  return /^https?:\/\//.test(completa) ? completa : new URL(completa, window.location.origin).toString();
}

/** Llama a la API y devuelve el cuerpo validado, o lanza un ErrorApi con el problema que respondió. */
export async function llamar<T = unknown>(ruta: string, opciones: OpcionesLlamada<T> = {}): Promise<T> {
  const binario = opciones.formato === 'blob';
  const cabeceras: Record<string, string> = {
    Accept: binario ? 'application/octet-stream, application/problem+json' : 'application/json, application/problem+json',
  };
  // Un FormData (subida de archivos) lleva su propio Content-Type multipart con el separador: no se fija aquí.
  const multipart = opciones.cuerpo instanceof FormData;
  if (opciones.cuerpo !== undefined && !multipart) cabeceras['Content-Type'] = 'application/json';
  if (opciones.token) cabeceras.Authorization = `Bearer ${opciones.token}`;

  let respuesta: Response;
  try {
    respuesta = await fetch(url(ruta), {
      method: opciones.metodo ?? 'GET',
      headers: cabeceras,
      body:
        opciones.cuerpo === undefined
          ? undefined
          : multipart
            ? (opciones.cuerpo as FormData)
            : JSON.stringify(opciones.cuerpo),
      credentials: opciones.credenciales ? 'include' : 'same-origin',
      signal: opciones.signal,
    });
  } catch {
    // Una cancelación de sesión no es una caída de red ni debe provocar un reintento con otra identidad.
    opciones.signal?.throwIfAborted();
    // fetch solo falla sin respuesta: red caída, servidor apagado o petición bloqueada (CSP, CORS).
    throw new ErrorApi(0, CODIGO_SIN_CONEXION, 'No hay conexión con el servidor.');
  }

  if (!respuesta.ok) {
    throw await comoErrorApi(respuesta);
  }
  if (respuesta.status === 204) {
    return undefined as T;
  }
  if (binario) {
    return (await respuesta.blob().catch(() => {
      opciones.signal?.throwIfAborted();
      throw new ErrorApi(0, CODIGO_SIN_CONEXION, 'No hay conexión con el servidor.');
    })) as T;
  }
  const cuerpo: unknown = await respuesta.json().catch(() => {
    opciones.signal?.throwIfAborted();
    throw new ErrorApi(respuesta.status, CODIGO_RESPUESTA_INVALIDA, 'La respuesta de la API no es JSON.');
  });
  if (!opciones.esquema) return cuerpo as T;
  const validado = opciones.esquema.safeParse(cuerpo);
  if (!validado.success) {
    // La forma no coincide con el contrato: es un defecto, no un dato que la pantalla deba mostrar.
    throw new ErrorApi(respuesta.status, CODIGO_RESPUESTA_INVALIDA, 'La respuesta de la API no sigue el contrato.');
  }
  return validado.data;
}

/** Códigos propios del cliente (no vienen de la API): sin respuesta y respuesta fuera del contrato. */
export const CODIGO_SIN_CONEXION = 'SIN_CONEXION';
export const CODIGO_RESPUESTA_INVALIDA = 'RESPUESTA_INVALIDA';

async function comoErrorApi(respuesta: Response): Promise<ErrorApi> {
  const tipo = respuesta.headers.get('Content-Type') ?? '';
  if (tipo.includes('json')) {
    const problema = esquemaProblema.safeParse(await respuesta.json().catch(() => null));
    if (problema.success) {
      return new ErrorApi(
        respuesta.status,
        problema.data.codigo ?? 'DESCONOCIDO',
        problema.data.detail ?? problema.data.title ?? 'Error de la API.',
        problema.data.errores ?? [],
        problema.data.faltantes ?? [],
      );
    }
  }
  // Sin cuerpo de problema (por ejemplo, un 502 del proxy): sin detalles técnicos.
  return new ErrorApi(respuesta.status, 'DESCONOCIDO', `La API respondió ${respuesta.status}.`);
}

export function esErrorApi(error: unknown, codigo?: string): error is ErrorApi {
  return error instanceof ErrorApi && (codigo === undefined || error.codigo === codigo);
}
