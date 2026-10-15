import { RotateCcwIcon } from 'lucide-react';
import { isRouteErrorResponse, useRouteError } from 'react-router';
import { mensajeDeError } from '@/api/mensajes';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';

// Pantalla para un error que nadie atrapó al pintar una ruta (errorElement de rutas.tsx). Reemplaza la página de
// React Router, que muestra la traza: aquí no se exponen detalles técnicos. Los errores de la API que cada pantalla
// ya maneja (carga, guardado, conflictos) no llegan aquí.
export function ErrorRuta() {
  const error = useRouteError();
  const texto = isRouteErrorResponse(error)
    ? 'La página no está disponible.'
    : mensajeDeError(error, 'Ocurrió un error inesperado en esta pantalla.');

  return (
    <main className="flex min-h-svh items-center justify-center bg-secondary p-4">
      <section className="flex w-full max-w-md flex-col gap-4">
        <h1 className="font-heading text-2xl font-bold text-primary">Algo salió mal</h1>
        <Alert variant="destructive">
          <AlertTitle>{texto}</AlertTitle>
          <AlertDescription>Recarga la página. Si el problema sigue, avisa al administrador.</AlertDescription>
        </Alert>
        <Button className="self-start" onClick={() => window.location.reload()}>
          <RotateCcwIcon data-icon="inline-start" />
          Recargar la página
        </Button>
      </section>
    </main>
  );
}
