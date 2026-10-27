import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { flujoBReal } from './flujosFase6';
import { sinSesionGuardada } from './apiFicticia';
import { estadisticasVacias } from './estadisticasFicticias';

// Servidor MSW de las pruebas: cada prueba registra las respuestas de la API que necesita con servidor.use(...).
// Por defecto no hay sesión que recuperar (/auth/renovar responde 401, como sin cookie). Una llamada sin respuesta
// registrada es un error, para no esconder llamadas inesperadas.
export const servidor = setupServer(
  sinSesionGuardada(),
  http.get('*/api/v1/flujos/EC/B', () => HttpResponse.json(flujoBReal)),
  // Superficies nuevas: los casos que las ejercitan reemplazan estos vacíos explícitos.
  http.get('*/api/v1/estadisticas/pedidos', ({ request }) =>
    HttpResponse.json(estadisticasVacias(Number(new URL(request.url).searchParams.get('anio'))))),
  http.get('*/api/v1/alertas', () => HttpResponse.json([])),
  http.get('*/api/v1/pedidos/:id/documentos', () => HttpResponse.json([])),
  http.get('*/api/v1/pedidos/:id/carpeta-drive', ({ params }) => HttpResponse.json({
    pedidoId: params.id, estado: 'SIN_PROGRAMAR', intentos: 0, ultimoError: null, programadaEn: null, creadaEn: null,
  })),
);
