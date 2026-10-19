import { useState } from 'react';
import { CopiasFallidas } from './pedidos/CopiasFallidas';
import { Link } from 'react-router';
import { type EstadoCorreo, useNotificaciones } from '@/api/notificaciones';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableHeader, TableHead, TableRow, TableBody, TableCell, TableCaption } from '@/components/ui/table';
import { formatearFechaHora, mensajeDeErrorPedido } from './pedidos/formato';

export function GmailNotificationsView() {
  const [estado, setEstado] = useState<EstadoCorreo>('PENDIENTE');
  const [pagina, setPagina] = useState(0);
  const registro = useNotificaciones(estado, pagina);
  return <Card>
    <CardHeader><CardTitle><h1>Registro de correos</h1></CardTitle>
      <CardDescription>Solo admin. Enviado significa aceptado por Gmail; no confirma recepción ni lectura.</CardDescription>
    </CardHeader>
    <CardContent className="flex flex-col gap-4">
      <Alert><AlertDescription>El procesamiento está desactivado por defecto. Faltan destinatarios admin aprobados.
        Los resultados inciertos requieren revisión; esta pantalla no reenvía correos.</AlertDescription></Alert>
      <FieldGroup><Field className="max-w-xs"><FieldLabel htmlFor="estado-correo">Estado</FieldLabel>
        <Select value={estado} onValueChange={(valor) => { setEstado(valor as EstadoCorreo); setPagina(0); }}>
          <SelectTrigger id="estado-correo"><SelectValue /></SelectTrigger>
          <SelectContent><SelectGroup>
            <SelectItem value="PENDIENTE">Pendientes</SelectItem>
            <SelectItem value="ENVIADA">Enviados</SelectItem>
            <SelectItem value="FALLIDA">Fallidos / inciertos</SelectItem>
          </SelectGroup></SelectContent>
        </Select></Field></FieldGroup>
      {registro.isPending ? <Skeleton className="h-32 w-full" aria-label="Cargando registro" />
        : registro.isError ? <Alert variant="destructive"><AlertDescription>
          {mensajeDeErrorPedido(registro.error, 'No se pudo consultar el registro de correos.')}
        </AlertDescription></Alert> : <Table>
          <TableCaption>Datos persistidos. No se generan mensajes desde esta pantalla.</TableCaption>
          <TableHeader><TableRow><TableHead>Pedido / asunto</TableHead><TableHead>Destinatario</TableHead>
            <TableHead>Resultado</TableHead><TableHead>Intentos</TableHead><TableHead>Fechas</TableHead>
          </TableRow></TableHeader>
          <TableBody>{registro.data.elementos.length === 0
            ? <TableRow><TableCell colSpan={5}>No hay correos en este estado.</TableCell></TableRow>
            : registro.data.elementos.map((n) => <TableRow key={n.id}>
              <TableCell><Link className="underline" to={`/pedidos/${n.pedidoId}`}>Ver pedido</Link><p>{n.asunto}</p></TableCell>
              <TableCell>{n.destinatario ?? 'Pendiente de aprobación'}</TableCell>
              <TableCell><Badge variant="outline">{n.resultadoIncierto ? 'Resultado incierto' : n.estado}</Badge>
                {n.bloqueo && <p>Bloqueado: falta destinatario aprobado.</p>}
                {n.ultimoError && <p>{n.ultimoError}</p>}
              </TableCell>
              <TableCell>{n.intentos}</TableCell>
              <TableCell><p>Creado: {formatearFechaHora(n.creadaEn)}</p>
                <p>Aceptado: {formatearFechaHora(n.enviadaEn)}</p></TableCell>
            </TableRow>)}</TableBody>
        </Table>}
      <CopiasFallidas />
    </CardContent>
    <CardFooter className="flex flex-wrap gap-3">
      <Button variant="outline" disabled={registro.isFetching} onClick={() => void registro.refetch()}>Actualizar</Button>
      <Button variant="outline" disabled={pagina === 0 || registro.isFetching} onClick={() => setPagina(p => p - 1)}>Anterior</Button>
      <span role="status">Página {pagina + 1}</span>
      <Button variant="outline" disabled={!registro.data || (pagina + 1) * 20 >= registro.data.total || registro.isFetching}
        onClick={() => setPagina(p => p + 1)}>Siguiente</Button>
    </CardFooter>
  </Card>;
}
