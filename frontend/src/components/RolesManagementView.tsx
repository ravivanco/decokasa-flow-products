import { zodResolver } from '@hookform/resolvers/zod';
import { PencilIcon, PlusIcon } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { esErrorApi } from '@/api/cliente';
import { useGuardarRol, useRoles } from '@/api/consultas';
import type { Rol } from '@/api/esquemas';
import { mensajeDeError } from '@/api/mensajes';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

// Gestión de roles del admin (plan, tarea 22; contrato: GET y POST /roles, PUT /roles/{rolId}). El código no cambia al
// editar: los flujos y las etapas lo usan. Los roles todavía no tienen versión (pendiente explícito de D10): dos
// administradores que editan el mismo rol a la vez se pisan; la última edición gana.

const esquemaRol = z.object({
  codigo: z.string().trim().regex(/^[a-z_]{3,40}$/, 'Usa de 3 a 40 letras minúsculas o guiones bajos, sin espacios.'),
  nombre: z.string().trim().min(1, 'Escribe el nombre del rol.').max(100, 'Máximo 100 caracteres.'),
  permisos: z
    .string()
    .refine(
      (texto) => separarPermisos(texto).every((permiso) => permiso.length <= 100),
      'Cada permiso tiene como máximo 100 caracteres.',
    ),
});
type DatosRol = z.infer<typeof esquemaRol>;

function separarPermisos(texto: string): string[] {
  return [...new Set(texto.split(',').map((permiso) => permiso.trim()).filter(Boolean))];
}

function FormularioRol({ rol, onCerrar }: { rol: Rol | null; onCerrar: () => void }) {
  const guardar = useGuardarRol();
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosRol>({
    resolver: zodResolver(esquemaRol),
    defaultValues: { codigo: rol?.codigo ?? '', nombre: rol?.nombre ?? '', permisos: rol?.permisos.join(', ') ?? '' },
  });

  async function enviar(datos: DatosRol) {
    setErrorGeneral(null);
    try {
      await guardar.mutateAsync({
        id: rol?.id,
        codigo: datos.codigo,
        nombre: datos.nombre,
        permisos: separarPermisos(datos.permisos),
      });
      onCerrar();
    } catch (error) {
      const campo = esErrorApi(error)
        ? error.errores.find((e) => ['codigo', 'nombre', 'permisos'].includes(e.campo))
        : undefined;
      if (campo) {
        setError(campo.campo as keyof DatosRol, { type: 'server', message: campo.mensaje }, { shouldFocus: true });
      } else {
        setErrorGeneral(mensajeDeError(error, 'No se pudo guardar el rol. Intenta de nuevo.'));
      }
    }
  }

  const invalido = (campo: keyof DatosRol) => (errors[campo] ? true : undefined);

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{rol ? 'Editar rol' : 'Nuevo rol'}</DialogTitle>
          <DialogDescription>
            Los permisos de cada rol todavía no están definidos; se acuerdan con los flujos de la fase 2.
          </DialogDescription>
        </DialogHeader>
        <form noValidate onSubmit={handleSubmit(enviar)} className="flex flex-col gap-5">
          {errorGeneral && (
            <Alert variant="destructive">
              <AlertDescription>{errorGeneral}</AlertDescription>
            </Alert>
          )}
          <FieldGroup>
            <Field data-invalid={invalido('codigo')}>
              <FieldLabel htmlFor="rol-codigo">Código</FieldLabel>
              <Input id="rol-codigo" readOnly={rol !== null} aria-invalid={invalido('codigo')} {...register('codigo')} />
              {rol !== null && <FieldDescription>El código no cambia: lo usan los flujos.</FieldDescription>}
              <FieldError errors={[errors.codigo]} />
            </Field>
            <Field data-invalid={invalido('nombre')}>
              <FieldLabel htmlFor="rol-nombre">Nombre</FieldLabel>
              <Input id="rol-nombre" aria-invalid={invalido('nombre')} {...register('nombre')} />
              <FieldError errors={[errors.nombre]} />
            </Field>
            <Field data-invalid={invalido('permisos')}>
              <FieldLabel htmlFor="rol-permisos">Permisos</FieldLabel>
              <Input id="rol-permisos" aria-invalid={invalido('permisos')} {...register('permisos')} />
              <FieldDescription>Separados por comas. Puede quedar vacío.</FieldDescription>
              <FieldError errors={[errors.permisos]} />
            </Field>
          </FieldGroup>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Guardando…' : 'Guardar'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

type Edicion = { rol: Rol | null } | null;

export function RolesManagementView() {
  const roles = useRoles();
  const [edicion, setEdicion] = useState<Edicion>(null);

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="font-heading text-2xl font-bold text-primary">Roles</h1>
        <Button onClick={() => setEdicion({ rol: null })}>
          <PlusIcon data-icon="inline-start" />
          Nuevo rol
        </Button>
      </div>

      {roles.isPending && (
        <p role="status" className="text-muted-foreground">
          Cargando roles…
        </p>
      )}
      {roles.isError && (
        <Alert variant="destructive">
          <AlertDescription>{mensajeDeError(roles.error, 'No se pudieron cargar los roles.')}</AlertDescription>
        </Alert>
      )}

      {roles.data && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Código</TableHead>
              <TableHead>Nombre</TableHead>
              <TableHead>Permisos</TableHead>
              <TableHead>
                <span className="sr-only">Acciones</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {roles.data.map((rol) => (
              <TableRow key={rol.id}>
                <TableCell className="font-mono text-sm">{rol.codigo}</TableCell>
                <TableCell className="font-medium">{rol.nombre}</TableCell>
                <TableCell>
                  {rol.permisos.length === 0 ? (
                    <span className="text-muted-foreground">Sin permisos</span>
                  ) : (
                    <div className="flex flex-wrap gap-1">
                      {rol.permisos.map((permiso) => (
                        <Badge key={permiso} variant="secondary">
                          {permiso}
                        </Badge>
                      ))}
                    </div>
                  )}
                </TableCell>
                <TableCell>
                  <div className="flex justify-end">
                    <Button
                      variant="outline"
                      size="sm"
                      aria-label={`Editar el rol ${rol.nombre}`}
                      onClick={() => setEdicion({ rol })}
                    >
                      <PencilIcon data-icon="inline-start" />
                      Editar
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      {edicion && <FormularioRol rol={edicion.rol} onCerrar={() => setEdicion(null)} />}
    </section>
  );
}
