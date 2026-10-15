import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import { esErrorApi } from '@/api/cliente';
import { useActualizarUsuario, useCrearUsuario, type CambiosUsuario } from '@/api/consultas';
import type { Rol, Usuario } from '@/api/esquemas';
import { mensajeDeError } from '@/api/mensajes';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Field, FieldError, FieldGroup, FieldLabel, FieldLegend, FieldSet } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { bytesUtf8 } from '@/lib/validacion';

// Alta y edición de usuarios (plan, tarea 22). Los límites son los del contrato y la base (SolicitudCrearUsuario):
// nombre 1–200, correo válido hasta 254, usuario 3–50, teléfono con 7 a 15 dígitos, al menos un rol y contraseña
// inicial de 8 caracteres a 72 bytes. La edición envía la versión leída y solo los campos cambiados (D10). Ante un 409
// VERSION_DESACTUALIZADA no reenvía nada: recarga el usuario y la persona elige descartar lo que escribió o
// reaplicarlo sobre los datos vigentes; en los dos casos guarda ella, con la versión vigente.

const camposComunes = {
  nombre: z.string().trim().min(1, 'Escribe el nombre.').max(200, 'Máximo 200 caracteres.'),
  correo: z.string().trim().max(254, 'Máximo 254 caracteres.').pipe(z.email('Escribe un correo válido.')),
  usuario: z.string().trim().min(3, 'El usuario debe tener entre 3 y 50 caracteres.').max(50, 'El usuario debe tener entre 3 y 50 caracteres.'),
  telefono: z
    .string()
    .trim()
    .refine(
      (valor) => /^\+?[0-9 ()-]{7,20}$/.test(valor) && /^\+?[0-9]{7,15}$/.test(valor.replace(/[ ()-]/g, '')),
      'Escribe un teléfono válido, de 7 a 15 dígitos.',
    ),
  roles: z.array(z.string()).min(1, 'Elige al menos un rol.'),
};

const esquemaEdicion = z.object(camposComunes);
const esquemaAlta = z.object({
  ...camposComunes,
  contrasenaInicial: z
    .string()
    .min(8, 'La contraseña inicial necesita al menos 8 caracteres.')
    .refine((valor) => bytesUtf8(valor) <= 72, 'La contraseña inicial tiene como máximo 72 bytes (una letra con tilde ocupa 2).'),
});

type DatosFormulario = z.infer<typeof esquemaAlta>;
type CampoFormulario = keyof DatosFormulario;
const CAMPOS: CampoFormulario[] = ['nombre', 'correo', 'usuario', 'telefono', 'roles', 'contrasenaInicial'];

function valoresDe(usuario: Usuario | null): DatosFormulario {
  return {
    nombre: usuario?.nombre ?? '',
    correo: usuario?.correo ?? '',
    usuario: usuario?.usuario ?? '',
    telefono: usuario?.telefono ?? '',
    roles: usuario?.roles ?? [],
    contrasenaInicial: '',
  };
}

const mismosRoles = (a: string[], b: string[]) => a.length === b.length && a.every((rol) => b.includes(rol));

/** Campos que se pueden editar (la contraseña inicial solo existe en el alta). */
const CAMPOS_EDITABLES = ['nombre', 'correo', 'usuario', 'telefono', 'roles'] as const;
type CampoEditable = (typeof CAMPOS_EDITABLES)[number];
const NOMBRES_CAMPOS: Record<CampoEditable, string> = {
  nombre: 'el nombre',
  correo: 'el correo',
  usuario: 'el usuario',
  telefono: 'el teléfono',
  roles: 'los roles',
};

/** Solo lo que cambió respecto de lo leído, con la versión leída. */
function cambiosEntre(base: Usuario, datos: DatosFormulario): CambiosUsuario {
  const cambios: CambiosUsuario = { id: base.id, version: base.version };
  if (datos.nombre !== base.nombre) cambios.nombre = datos.nombre;
  if (datos.correo !== base.correo) cambios.correo = datos.correo;
  if (datos.usuario !== base.usuario) cambios.usuario = datos.usuario;
  if (datos.telefono !== base.telefono) cambios.telefono = datos.telefono;
  if (!mismosRoles(datos.roles, base.roles)) cambios.roles = datos.roles;
  return cambios;
}

interface Props {
  /** null para un alta; el usuario tal como se leyó para una edición. */
  usuario: Usuario | null;
  roles: Rol[];
  /** Trae de nuevo la lista y devuelve el usuario vigente (tras un 409 VERSION_DESACTUALIZADA). */
  recargar: (id: string) => Promise<Usuario | undefined>;
  onCerrar: () => void;
}

export function FormularioUsuario({ usuario, roles, recargar, onCerrar }: Props) {
  const esAlta = usuario === null;
  const [base, setBase] = useState(usuario);
  const [conflicto, setConflicto] = useState(false);
  /** El usuario vigente en el servidor, recargado solo al recibir un 409 VERSION_DESACTUALIZADA. */
  const [vigente, setVigente] = useState<Usuario | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const crear = useCrearUsuario();
  const actualizar = useActualizarUsuario();

  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormulario>({
    // En la edición no hay contraseña: se valida con el esquema sin ella.
    resolver: zodResolver(esAlta ? esquemaAlta : (esquemaEdicion as unknown as typeof esquemaAlta)),
    defaultValues: valoresDe(usuario),
  });

  function mostrarError(error: unknown) {
    if (esErrorApi(error, 'VERSION_DESACTUALIZADA')) {
      // No se reenvía nada. Se recarga el usuario para que la persona decida con los datos vigentes; lo que escribió
      // sigue en el formulario hasta que elija descartarlo o reaplicarlo.
      setConflicto(true);
      setVigente(null);
      if (base !== null) {
        recargar(base.id)
          .then((actual) => setVigente(actual ?? null))
          .catch(() => setVigente(null));
      }
      return;
    }
    if (esErrorApi(error) && error.errores.length > 0) {
      let enCampo = false;
      for (const { campo, mensaje } of error.errores) {
        if ((CAMPOS as string[]).includes(campo)) {
          setError(campo as CampoFormulario, { type: 'server', message: mensaje }, { shouldFocus: !enCampo });
          enCampo = true;
        }
      }
      if (enCampo) return;
    }
    setErrorGeneral(mensajeDeError(error, 'No se pudo guardar. Intenta de nuevo.'));
  }

  async function guardar(datos: DatosFormulario) {
    setErrorGeneral(null);
    setAviso(null);
    try {
      if (base === null) {
        const { contrasenaInicial, ...resto } = datos;
        await crear.mutateAsync({ ...resto, contrasenaInicial, paises: ['EC'] });
      } else {
        const cambios = cambiosEntre(base, datos);
        // Sin cambios no hay nada que enviar (un PATCH con solo la versión es 400).
        if (Object.keys(cambios).length > 2) await actualizar.mutateAsync(cambios);
      }
      onCerrar();
    } catch (error) {
      mostrarError(error);
    }
  }

  /** El usuario vigente: el que ya se recargó tras el 409 o, si todavía no llegó, uno pedido ahora. */
  async function obtenerVigente(): Promise<Usuario | null> {
    if (vigente) return vigente;
    if (base === null) return null;
    const actual = (await recargar(base.id).catch(() => undefined)) ?? null;
    setVigente(actual);
    if (!actual) setErrorGeneral('No se pudieron cargar los datos actuales. Cierra el diálogo y vuelve a abrirlo.');
    return actual;
  }

  function retomarCon(actual: Usuario, valores: DatosFormulario) {
    setBase(actual);
    reset(valores);
    setConflicto(false);
    setVigente(null);
    setErrorGeneral(null);
  }

  /** Descarta lo escrito y muestra los datos vigentes, con su versión, para editar de nuevo. */
  async function cargarDatosActuales() {
    const actual = await obtenerVigente();
    if (!actual) return;
    retomarCon(actual, valoresDe(actual));
    setAviso(null);
  }

  /**
   * Pone sobre los datos vigentes solo los campos que la persona cambió, sin enviar nada: revisa y pulsa Guardar, que
   * usará la versión vigente. Si otra persona cambió alguno de esos mismos campos, lo avisa.
   */
  async function reaplicarMisCambios() {
    if (base === null) return;
    const actual = await obtenerVigente();
    if (!actual) return;
    const mios = cambiosEntre(base, getValues());
    const valores = valoresDe(actual);
    const pisados: string[] = [];
    for (const campo of CAMPOS_EDITABLES) {
      if (mios[campo] === undefined) continue;
      (valores as Record<CampoEditable, unknown>)[campo] = mios[campo];
      const otroCambio =
        campo === 'roles' ? !mismosRoles(actual.roles, base.roles) : actual[campo] !== base[campo];
      if (otroCambio) pisados.push(NOMBRES_CAMPOS[campo]);
    }
    retomarCon(actual, valores);
    setAviso(
      pisados.length > 0
        ? `Otra persona también cambió ${pisados.join(', ')}. Quedaron tus valores: revísalos antes de guardar.`
        : 'Tus cambios quedaron sobre los datos actuales. Revísalos y pulsa Guardar.',
    );
  }

  const invalido = (campo: CampoFormulario) => (errors[campo] ? true : undefined);

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{esAlta ? 'Nuevo usuario' : 'Editar usuario'}</DialogTitle>
          <DialogDescription>
            {esAlta
              ? 'La persona entra con la contraseña inicial; compártela por un canal seguro.'
              : 'Solo se guardan los campos que cambies. Para retirar el acceso, desactiva la cuenta.'}
          </DialogDescription>
        </DialogHeader>
        <form noValidate onSubmit={handleSubmit(guardar)} className="flex flex-col gap-5">
          {conflicto && (
            <Alert variant="destructive">
              <AlertDescription className="flex flex-col items-start gap-3">
                <p>
                  Otra persona modificó este usuario. Tus cambios no se guardaron ni se enviarán solos.{' '}
                  {vigente
                    ? 'Ya se cargaron los datos actuales: descarta lo que escribiste o reaplícalo sobre ellos.'
                    : 'Cargando los datos actuales…'}
                </p>
                <div className="flex flex-wrap gap-2">
                  <Button type="button" variant="outline" size="sm" onClick={cargarDatosActuales}>
                    Cargar datos actuales
                  </Button>
                  <Button type="button" variant="outline" size="sm" onClick={reaplicarMisCambios} disabled={!vigente}>
                    Reaplicar mis cambios
                  </Button>
                </div>
              </AlertDescription>
            </Alert>
          )}
          {aviso && (
            <Alert>
              <AlertDescription>{aviso}</AlertDescription>
            </Alert>
          )}
          {errorGeneral && (
            <Alert variant="destructive">
              <AlertDescription>{errorGeneral}</AlertDescription>
            </Alert>
          )}
          <FieldGroup>
            {(
              [
                ['nombre', 'Nombre', 'name'],
                ['correo', 'Correo', 'email'],
                ['usuario', 'Usuario', 'username'],
                ['telefono', 'Teléfono', 'tel'],
              ] as const
            ).map(([campo, etiqueta, autoComplete]) => (
              <Field key={campo} data-invalid={invalido(campo)}>
                <FieldLabel htmlFor={`usuario-${campo}`}>{etiqueta}</FieldLabel>
                <Input
                  id={`usuario-${campo}`}
                  autoComplete={autoComplete === 'username' ? 'off' : autoComplete}
                  type={campo === 'correo' ? 'email' : campo === 'telefono' ? 'tel' : 'text'}
                  aria-invalid={invalido(campo)}
                  {...register(campo)}
                />
                <FieldError errors={[errors[campo]]} />
              </Field>
            ))}
            {esAlta && (
              <Field data-invalid={invalido('contrasenaInicial')}>
                <FieldLabel htmlFor="usuario-contrasenaInicial">Contraseña inicial</FieldLabel>
                <Input
                  id="usuario-contrasenaInicial"
                  type="password"
                  autoComplete="new-password"
                  aria-invalid={invalido('contrasenaInicial')}
                  {...register('contrasenaInicial')}
                />
                <FieldError errors={[errors.contrasenaInicial]} />
              </Field>
            )}
            <Controller
              control={control}
              name="roles"
              render={({ field, fieldState }) => (
                <FieldSet data-invalid={fieldState.invalid ? true : undefined}>
                  <FieldLegend variant="label">Roles</FieldLegend>
                  <div className="grid gap-3 sm:grid-cols-2">
                    {roles.map((rol) => {
                      const marcado = field.value.includes(rol.codigo);
                      return (
                        <Field key={rol.codigo} orientation="horizontal">
                          <Checkbox
                            id={`rol-${rol.codigo}`}
                            checked={marcado}
                            aria-invalid={fieldState.invalid ? true : undefined}
                            onCheckedChange={(valor) =>
                              field.onChange(
                                valor === true
                                  ? [...field.value, rol.codigo]
                                  : field.value.filter((codigo) => codigo !== rol.codigo),
                              )
                            }
                          />
                          <FieldLabel htmlFor={`rol-${rol.codigo}`} className="font-normal">
                            {rol.nombre}
                          </FieldLabel>
                        </Field>
                      );
                    })}
                  </div>
                  <FieldError errors={[fieldState.error]} />
                </FieldSet>
              )}
            />
          </FieldGroup>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting || conflicto}>
              {isSubmitting ? 'Guardando…' : 'Guardar'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
