import { z } from 'zod';

// La CSP del frontend (M4) prohíbe eval. Zod 4 prueba `new Function` para compilar validadores más rápidos; aunque
// atrapa el error, el navegador informa la prueba como violación de la CSP. En modo jitless no la hace.
// main.tsx importa este módulo antes que cualquier otro que use Zod.
z.config({ jitless: true });
