import { type FormEvent, useId, useState } from 'react';
import { LoaderIcon, PlusIcon, RotateCwIcon, WarehouseIcon } from 'lucide-react';
import { toast } from 'sonner';
import { useBodegas, useGuardarBodega } from '@/api/bodegas';
import { esErrorApi } from '@/api/cliente';
import type { Bodega } from '@/api/esquemas';
import { mensajeDeError } from '@/api/mensajes';
import { useUsuarioActual } from '@/auth/SesionProvider';
import { SinPermiso } from '@/components/PaginasSimples';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

/** Reutiliza la búsqueda/tabla/edición del catálogo existente, sustituyendo su estado ficticio por la API. */
export function CatalogoBodegas() {
  const usuario = useUsuarioActual();
  // Es una pista de navegación. POST/PATCH vuelven a autorizar cuenta y roles vigentes en el servidor.
  if (!usuario.roles.some((rol) => rol === 'admin' || rol === 'logistica')) return <SinPermiso />;
  return <ContenidoCatalogo />;
}

function ContenidoCatalogo() {
  const bodegas = useBodegas();
  const [texto, setTexto] = useState('');
  const [edicion, setEdicion] = useState<Bodega | 'nueva' | null>(null);
  const termino = texto.trim().toLocaleLowerCase('es');
  const visibles = bodegas.data?.filter((b) => `${b.nombre} ${b.ciudad}`.toLocaleLowerCase('es').includes(termino)) ?? [];
  return (
    <section className="flex min-w-0 flex-col gap-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-2">
          <h1 className="font-heading text-2xl font-bold text-primary">Bodegas</h1>
          <p className="max-w-prose text-muted-foreground">Nombre, ciudad y disponibilidad para nuevas asignaciones.</p>
        </div>
        <Button onClick={() => setEdicion('nueva')}><PlusIcon data-icon="inline-start" />Nueva bodega</Button>
      </header>
      <Alert role="note"><WarehouseIcon /><AlertTitle>Las asignaciones existentes se conservan</AlertTitle>
        <AlertDescription>Desactivar una bodega impide nuevas asignaciones. Los pedidos que ya la tienen no cambian.</AlertDescription>
      </Alert>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <Field className="max-w-md">
          <FieldLabel htmlFor="buscar-bodega">Buscar por nombre o ciudad</FieldLabel>
          <Input id="buscar-bodega" type="search" value={texto} onChange={(e) => setTexto(e.target.value)} />
        </Field>
        <Button variant="outline" disabled={bodegas.isFetching} onClick={() => void bodegas.refetch()}>
          <RotateCwIcon data-icon="inline-start" />Actualizar
        </Button>
      </div>
      {bodegas.isPending ? <div aria-busy="true" aria-label="Cargando bodegas"><Skeleton className="h-48 w-full" /></div>
        : bodegas.isError ? <Alert variant="destructive"><AlertTitle>No se pudo cargar el catálogo</AlertTitle>
          <AlertDescription>{mensajeDeError(bodegas.error, 'Revisa la conexión y pulsa Actualizar.')}</AlertDescription></Alert>
        : <Table>
          <TableCaption>{visibles.length} {visibles.length === 1 ? 'bodega' : 'bodegas'} · catálogo del servidor</TableCaption>
          <TableHeader><TableRow><TableHead>Nombre</TableHead><TableHead>Ciudad</TableHead><TableHead>Estado</TableHead><TableHead className="text-right">Mantenimiento</TableHead></TableRow></TableHeader>
          <TableBody>{visibles.length === 0
            ? <TableRow><TableCell colSpan={4} className="py-10 text-center text-muted-foreground">{texto ? 'Ninguna bodega coincide con la búsqueda.' : 'Todavía no hay bodegas registradas.'}</TableCell></TableRow>
            : visibles.map((bodega) => <TableRow key={bodega.id}>
              <TableCell className="max-w-64 whitespace-normal break-words font-medium">{bodega.nombre}</TableCell>
              <TableCell className="max-w-56 whitespace-normal break-words">{bodega.ciudad}</TableCell>
              <TableCell><Badge variant={bodega.activa ? 'secondary' : 'outline'}>{bodega.activa ? 'Activa' : 'Inactiva'}</Badge></TableCell>
              <TableCell className="text-right"><Button variant="outline" size="sm" onClick={() => setEdicion(bodega)} aria-label={`Editar bodega ${bodega.nombre}`}>Editar</Button></TableCell>
            </TableRow>)}</TableBody>
        </Table>}
      {edicion && <FormularioBodega key={edicion === 'nueva' ? 'nueva' : edicion.id} bodega={edicion === 'nueva' ? undefined : edicion} onCerrar={() => setEdicion(null)} />}
    </section>
  );
}

/** Conserva la versión y el borrador con los que se abrió; una recarga no sobrescribe lo que escribe la persona. */
function FormularioBodega({ bodega, onCerrar }: { bodega?: Bodega; onCerrar: () => void }) {
  const ids = useId();
  const guardar = useGuardarBodega();
  const [nombre, setNombre] = useState(bodega?.nombre ?? '');
  const [ciudad, setCiudad] = useState(bodega?.ciudad ?? '');
  const [activa, setActiva] = useState(bodega?.activa ?? true);
  const [errores, setErrores] = useState<{ nombre?: string; ciudad?: string }>({});
  const [fallo, setFallo] = useState<unknown>(null);
  const conflicto = esErrorApi(fallo, 'VERSION_DESACTUALIZADA');
  const sinPermiso = esErrorApi(fallo) && fallo.status === 403;
  function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (guardar.isPending || conflicto || sinPermiso) return;
    const nuevos: typeof errores = {};
    if (!nombre.trim() || nombre.trim().length > 100) nuevos.nombre = 'Escribe un nombre de 1 a 100 caracteres.';
    if (!ciudad.trim() || ciudad.trim().length > 100) nuevos.ciudad = 'Escribe una ciudad de 1 a 100 caracteres.';
    setErrores(nuevos);if (Object.keys(nuevos).length) return;
    setFallo(null);
    const datos = { nombre: nombre.trim(), ciudad: ciudad.trim(), activa };
    guardar.mutate(bodega ? { ...datos, id: bodega.id, version: bodega.version } : datos, {
      onSuccess: () => { toast.success(bodega ? 'Bodega actualizada.' : 'Bodega creada.');onCerrar(); },
      onError: (error) => {
        setFallo(error);
        if (esErrorApi(error, 'DUPLICADO')) setErrores({ nombre: 'Ya existe una bodega con ese nombre.' });
      },
    });
  }
  const mensaje = conflicto ? 'Otra persona modificó esta bodega. Conserva tu texto si lo necesitas; cierra y vuelve a abrir la versión actualizada antes de guardar.'
    : esErrorApi(fallo, 'DUPLICADO') ? 'Ese nombre ya está registrado.'
    : esErrorApi(fallo, 'VALIDACION') ? fallo.errores.map((e) => e.mensaje).join(' ') || 'Revisa los campos de la bodega.'
    : mensajeDeError(fallo, 'No se pudo guardar la bodega. Puedes intentarlo de nuevo.');
  return (
    <Dialog open onOpenChange={(abierta) => !abierta && !guardar.isPending && onCerrar()}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader><DialogTitle>{bodega ? 'Editar bodega' : 'Nueva bodega'}</DialogTitle>
          <DialogDescription>{bodega ? 'Se conservarán los pedidos ya asignados.' : 'Se guardará en el catálogo compartido de bodegas.'}</DialogDescription></DialogHeader>
        <form noValidate onSubmit={enviar} className="flex flex-col gap-6">
          <FieldGroup>
            <Field data-invalid={errores.nombre ? true : undefined}><FieldLabel htmlFor={`${ids}-nombre`}>Nombre</FieldLabel>
              <Input id={`${ids}-nombre`} value={nombre} maxLength={100} disabled={guardar.isPending || sinPermiso} readOnly={conflicto} onChange={(e) => setNombre(e.target.value)} aria-invalid={errores.nombre ? true : undefined} />
              <FieldError>{errores.nombre}</FieldError></Field>
            <Field data-invalid={errores.ciudad ? true : undefined}><FieldLabel htmlFor={`${ids}-ciudad`}>Ciudad</FieldLabel>
              <Input id={`${ids}-ciudad`} value={ciudad} maxLength={100} disabled={guardar.isPending || sinPermiso} readOnly={conflicto} onChange={(e) => setCiudad(e.target.value)} aria-invalid={errores.ciudad ? true : undefined} />
              <FieldError>{errores.ciudad}</FieldError></Field>
            <Field orientation="horizontal"><Checkbox id={`${ids}-activa`} checked={activa} disabled={guardar.isPending || conflicto || sinPermiso} onCheckedChange={(valor) => setActiva(valor === true)} />
              <FieldLabel htmlFor={`${ids}-activa`}>Disponible para nuevas asignaciones</FieldLabel></Field>
            <FieldDescription>No se eliminan bodegas ni se cambian asignaciones existentes.</FieldDescription>
          </FieldGroup>
          {fallo !== null && <Alert variant="destructive" aria-live="polite"><AlertDescription>{mensaje}</AlertDescription></Alert>}
          <DialogFooter><Button type="button" variant="outline" disabled={guardar.isPending} onClick={onCerrar}>{conflicto ? 'Cerrar y revisar versión actual' : 'Cancelar'}</Button>
            <Button type="submit" disabled={guardar.isPending || conflicto || sinPermiso}>{guardar.isPending && <LoaderIcon data-icon="inline-start" className="animate-spin" />}Guardar bodega</Button></DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
