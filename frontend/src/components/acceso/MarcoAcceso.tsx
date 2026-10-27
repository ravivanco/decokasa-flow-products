import type { ReactNode } from 'react';
import logo from '@/assets/marca/logo1.jpeg';

// Marco de las pantallas sin sesión (login, recuperar y restablecer; ADR-0033). Usa el logo original del cliente
// (documentos/logos/logo1.jpeg, sin modificar) sobre su mismo morado: a la izquierda en escritorio y como franja
// superior en móvil. El nombre de la marca está en el logo; los textos no lo repiten.
export function MarcoAcceso({ titulo, descripcion, children }: { titulo: string; descripcion?: ReactNode; children: ReactNode }) {
  return (
    <div className="grid min-h-svh grid-rows-[auto_1fr] bg-background lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)] lg:grid-rows-1">
      <div className="flex flex-col items-center justify-center gap-4 bg-marca-morado px-6 py-3 lg:py-12">
        {/* En móvil se recorta al mostrarlo solo el margen morado vacío de los lados, para que el logo se lea. */}
        <div className="h-44 w-52 overflow-hidden sm:h-52 sm:w-60 lg:h-auto lg:w-full lg:max-w-2xl">
          <img
            src={logo}
            width={1416}
            height={648}
            alt="Latinoamérica World. Importando y Exportando el Mundo."
            className="size-full object-cover lg:h-auto lg:object-contain"
          />
        </div>
        {/* <p className="hidden max-w-sm text-center text-sm text-white/85 lg:block">
          Seguimiento de pedidos de importación entre Ecuador y China.
        </p> */}
      </div>
      <main className="flex items-start justify-center px-4 py-10 sm:px-8 lg:items-center">
        <div className="flex w-full max-w-sm flex-col gap-6">
          <div className="flex flex-col gap-1.5">
            <h1 className="font-heading text-2xl font-bold text-primary">{titulo}</h1>
            {descripcion && <p className="text-sm text-muted-foreground">{descripcion}</p>}
          </div>
          {children}
        </div>
      </main>
    </div>
  );
}
