import { http, HttpResponse } from 'msw';

// Datos y respuestas ficticias con la forma del contrato (documentos/api/openapi.yaml). Nunca datos reales.

export const CLAVE = 'Clave-Prueba-1';

export interface UsuarioApi {
  id: string;
  nombre: string;
  correo: string;
  usuario: string;
  telefono: string;
  roles: string[];
  paises: string[];
  activo: boolean;
}

function usuarioFicticio(numero: number, nombre: string, roles: string[], activo = true): UsuarioApi {
  const corto = nombre.split(' ')[0].toLowerCase();
  return {
    id: `00000000-0000-4000-8000-${String(numero).padStart(12, '0')}`,
    nombre,
    correo: `${corto}@ejemplo.test`,
    usuario: `${corto}prueba`,
    telefono: `+5939900000${String(numero).padStart(2, '0')}`,
    roles,
    paises: ['EC'],
    activo,
  };
}

export const admin = usuarioFicticio(10, 'Admin Prueba', ['admin']);
export const mario = usuarioFicticio(11, 'Mario Prueba', ['marketing']);
export const ana = usuarioFicticio(1, 'Ana Prueba', ['compras_china', 'logistica']);
export const beto = usuarioFicticio(2, 'Beto Prueba', ['logistica'], false);
export const eva = usuarioFicticio(12, 'Eva Prueba', ['compras_china', 'admin']);

export const rolesApi = [
  { codigo: 'admin', nombre: 'Administrador' },
  { codigo: 'asistente_compras', nombre: 'Asistente de compras' },
  { codigo: 'marketing', nombre: 'Marketing y codificación' },
  { codigo: 'compras_china', nombre: 'Compras China' },
  { codigo: 'logistica', nombre: 'Logística' },
  { codigo: 'asistente_china', nombre: 'Asistente de compras en China' },
  { codigo: 'consulta', nombre: 'Consulta' },
].map((rol, indice) => ({
  id: `01a12d68-b800-7000-8000-00000000000${indice + 1}`,
  ...rol,
  permisos: [] as string[],
}));

export function problema(
  status: number,
  codigo: string,
  detail: string,
  errores?: { campo: string; mensaje: string }[],
) {
  return HttpResponse.json(
    { type: 'about:blank', title: detail, status, detail, codigo, ...(errores ? { errores } : {}) },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );
}

/** Login correcto y /auth/yo para el usuario dado (contrato: RespuestaSesion y UsuarioActual). */
export function sesionDe(usuario: UsuarioApi) {
  return [
    http.post('*/api/v1/auth/login', () =>
      HttpResponse.json({ tokenAcceso: `token-de-prueba-${usuario.usuario}`, expiraEn: 900, usuario }),
    ),
    http.get('*/api/v1/auth/yo', () => HttpResponse.json({ ...usuario, permisos: [] })),
  ];
}

/** Sin cookie de renovación válida: no hay sesión que recuperar al recargar (D3b). */
export function sinSesionGuardada() {
  return http.post('*/api/v1/auth/renovar', () => problema(401, 'NO_AUTENTICADO', 'La sesión terminó.'));
}

/**
 * Hay una cookie de renovación válida: /auth/renovar entrega un acceso nuevo, como al recargar la página (D3b).
 * Cada llamada devuelve un token distinto (token-renovado-1, -2…); `llamadas` cuenta cuántas hubo.
 */
export function sesionGuardada(usuario: UsuarioApi) {
  const registro = { llamadas: 0 };
  const manejadores = [
    http.post('*/api/v1/auth/renovar', () => {
      registro.llamadas++;
      return HttpResponse.json({ tokenAcceso: `token-renovado-${registro.llamadas}`, expiraEn: 900, usuario });
    }),
    http.get('*/api/v1/auth/yo', () => HttpResponse.json({ ...usuario, permisos: [] })),
  ];
  return { manejadores, registro };
}

/** Lista de usuarios y catálogo de roles que ve el admin. */
export function catalogos(usuarios: UsuarioApi[] = [admin, mario, ana, beto]) {
  return [
    http.get('*/api/v1/usuarios', () => HttpResponse.json(usuarios)),
    http.get('*/api/v1/roles', () => HttpResponse.json(rolesApi)),
  ];
}
