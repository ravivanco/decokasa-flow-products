import { Link } from 'react-router';
import { Button } from '@/components/ui/button';

// Páginas de aviso dentro de la aplicación con sesión: un h1 por pantalla (DESIGN.md 8).

function Aviso({ titulo, texto }: { titulo: string; texto: string }) {
  return (
    <section className="flex flex-col items-start gap-4">
      <h1 className="font-heading text-2xl font-bold text-primary">{titulo}</h1>
      <p className="text-muted-foreground">{texto}</p>
      <Button asChild variant="outline">
        <Link to="/inicio">Ir al inicio</Link>
      </Button>
    </section>
  );
}

export function SinPermiso() {
  return <Aviso titulo="Sin permiso" texto="Tu rol no permite usar esta sección. Si la necesitas, pídela al administrador." />;
}

export function PaginaNoEncontrada() {
  return <Aviso titulo="Página no encontrada" texto="La dirección no existe o todavía no está disponible." />;
}
