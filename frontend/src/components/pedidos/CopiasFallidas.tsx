import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { z } from 'zod';
import { Link } from 'react-router';
import { useSesion } from '@/auth/SesionProvider';
import { segmentoPais, usePaisActivo } from '@/pais/PaisProvider';
import { esquemaAdjunto } from '@/api/pedidos';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableCaption, TableHeader, TableHead, TableRow, TableBody, TableCell } from '@/components/ui/table';
import { mensajeDeErrorPedido } from './formato';

export function CopiasFallidas() {
  const { pedir, sesionId } = useSesion();
  // Paso 7.3: solo archivos de pedidos del país activo. El total sigue pendiente (el contrato responde una lista).
  const pais = usePaisActivo();
  const [pagina, setPagina] = useState(0);
  const copias = useQuery({
    queryKey: ['copias-fallidas', sesionId, segmentoPais(pais), pagina],
    queryFn: ({ signal }) => pedir(`/archivos/copias-fallidas?pais=${pais}&pagina=${pagina}&tamano=20`, {
      esquema: z.array(esquemaAdjunto), signal,
    }), staleTime: 10_000, retry: false,
  });
  return <section aria-labelledby="copias-fallidas" className="flex flex-col gap-3">
    <h2 id="copias-fallidas">Copias a Drive fallidas</h2>
    {copias.isPending ? <Skeleton className="h-20 w-full" aria-label="Cargando copias fallidas" />
      : copias.isError ? <Alert variant="destructive"><AlertDescription>
        {mensajeDeErrorPedido(copias.error, 'No se pudieron consultar las copias fallidas.')}
      </AlertDescription></Alert> : <Table>
        <TableCaption>El archivo aceptado se conserva; la descarga sigue en el pedido.</TableCaption>
        <TableHeader><TableRow><TableHead>Archivo</TableHead><TableHead>Etapa</TableHead><TableHead>Pedido</TableHead></TableRow></TableHeader>
        <TableBody>{copias.data.length === 0 ? <TableRow><TableCell colSpan={3}>No hay copias fallidas en esta página.</TableCell></TableRow>
          : copias.data.map(a => <TableRow key={a.id}><TableCell>{a.nombre}</TableCell><TableCell>{a.etapa}</TableCell>
            <TableCell><Link className="underline" to={`/pedidos/${a.pedidoId}`}>Ver pedido y descargar</Link></TableCell></TableRow>)}</TableBody>
      </Table>}
    <div className="flex flex-wrap gap-3">
      <Button variant="outline" disabled={copias.isFetching} onClick={() => void copias.refetch()}>Actualizar copias</Button>
      <Button variant="outline" disabled={!pagina || copias.isFetching} onClick={() => setPagina(p => p - 1)}>Anterior</Button>
      <span role="status">Página {pagina + 1}</span>
      <Button variant="outline" disabled={copias.data?.length !== 20 || copias.isFetching} onClick={() => setPagina(p => p + 1)}>Siguiente</Button>
    </div>
  </section>;
}
