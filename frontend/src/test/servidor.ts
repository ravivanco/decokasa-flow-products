import { setupServer } from 'msw/node';
import { sinSesionGuardada } from './apiFicticia';

// Servidor MSW de las pruebas: cada prueba registra las respuestas de la API que necesita con servidor.use(...).
// Por defecto no hay sesión que recuperar (/auth/renovar responde 401, como sin cookie). Una llamada sin respuesta
// registrada es un error, para no esconder llamadas inesperadas.
export const servidor = setupServer(sinSesionGuardada());
