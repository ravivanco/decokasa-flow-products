// Configuración común de Vitest: agrega los comparadores de Testing Library (toBeInTheDocument, etc.)
// y el servidor MSW que simula la API en las pruebas.
import '@testing-library/jest-dom/vitest';
import { servidor } from './servidor';

beforeAll(() => servidor.listen({ onUnhandledFrame: 'error' }));
afterEach(() => servidor.resetHandlers());
afterAll(() => servidor.close());
