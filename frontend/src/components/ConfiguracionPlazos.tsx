import { zodResolver } from '@hookform/resolvers/zod';
import { CircleAlertIcon, LoaderIcon, RotateCwIcon, SaveIcon, TargetIcon } from 'lucide-react';
import { useId, useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { z } from 'zod';
import { esErrorApi } from '@/api/cliente';
import { mensajeDeError } from '@/api/mensajes';
import { type ConfiguracionPlazos as Configuracion, useActualizarMeta, useConfiguracionPlazos } from '@/api/plazos';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { formatearFechaHora } from './pedidos/formato';

// Meta de días (cierre de la parte A de la fase 5; F5-D6). Pantalla solo del admin (SoloAdmin en la ruta; el servidor
// vuelve a exigir admin con el estado vigente). La meta se presenta siempre como propuesta pendiente del cliente:
// guardarla no la vuelve un objetivo aprobado ni permite declarar cumplimiento. Ante 409 VERSION_DESACTUALIZADA se
// conserva lo escrito, se muestra el valor vigente y la persona elige; nada se reenvía solo. Los feriados siguen
// pendientes del cliente y no se configuran aquí.

const MAXIMO = 2_147_483_647;

const esquema = z.object({
  metaDias: z
    .string()
    .trim()
    .regex(/^[1-9]\d*$/, 'Escribe la meta en días: un número entero desde 1.')
    .refine((valor) => Number(valor) <= MAXIMO, 'La meta es demasiado grande.'),
});
type Datos = z.infer<typeof esquema>;

export function ConfiguracionPlazos() {
  const configuracion = useConfiguracionPlazos();

  return (
    <section className="flex max-w-3xl flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-bold text-primary">Meta de plazos</h1>
        <p className="text-muted-foreground">
          Días de pedido a llegada con los que el cronograma compara cada pedido.
        </p>
      </div>
      {configuracion.isPending ? (
        <div className="flex flex-col gap-3" aria-busy="true" aria-label="Cargando la configuración">
          <Skeleton className="h-8 w-64" />
          <Skeleton className="h-40 w-full" />
        </div>
      ) : configuracion.isError ? (
        <Alert variant="destructive">
          <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
            <p>{mensajeDeError(configuracion.error, 'No se pudo cargar la configuración de plazos.')}</p>
            <Button variant="outline" size="sm" onClick={() => void configuracion.refetch()}>
              <RotateCwIcon data-icon="inline-start" />
              Reintentar
            </Button>
          </AlertDescription>
        </Alert>
      ) : (
        <FormularioMeta inicial={configuracion.data} vigente={configuracion.data} releyendo={configuracion.isFetching} />
      )}
    </section>
  );
}

function FormularioMeta({
  inicial,
  vigente,
  releyendo,
}: {
  inicial: Configuracion;
  vigente: Configuracion;
  releyendo: boolean;
}) {
  const ids = useId();
  const actualizar = useActualizarMeta();
  // La versión que la persona leyó; solo cambia al guardar o cuando ella decide tras un conflicto.
  const [versionLeida, setVersionLeida] = useState(inicial.version);
  const [conflicto, setConflicto] = useState(false);
  const [fallo, setFallo] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<Datos>({
    resolver: zodResolver(esquema),
    defaultValues: { metaDias: String(inicial.metaDias) },
  });
  const vigenteDistinta = vigente.version !== versionLeida;

  function guardar(datos: Datos) {
    setFallo(null);
    actualizar.mutate(
      { metaDias: Number(datos.metaDias), version: versionLeida },
      {
        onSuccess: (guardada) => {
          setVersionLeida(guardada.version);
          setConflicto(false);
          reset({ metaDias: String(guardada.metaDias) });
          toast.success('Meta guardada. Sigue siendo una propuesta pendiente del cliente.');
        },
        onError: (error) => {
          if (esErrorApi(error, 'VERSION_DESACTUALIZADA')) setConflicto(true);
          else setFallo(error);
        },
      },
    );
  }

  return (
    <Card>
      <CardHeader className="gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <CardTitle>
            <h2 className="flex items-center gap-2 font-heading text-xl font-semibold">
              <TargetIcon aria-hidden="true" className="size-5" />
              Meta propuesta
            </h2>
          </CardTitle>
          <Badge variant="outline" className="h-auto rounded-sm py-0.5 text-sm">
            {vigente.origenMeta === 'ADMIN'
              ? 'Configurada por el admin · pendiente del cliente'
              : 'Propuesta · pendiente del cliente'}
          </Badge>
        </div>
        <CardDescription>
          Vigente: <span className="font-medium tabular-nums">{vigente.metaDias}</span> días
          {vigente.actualizadoEn
            ? ` · actualizada el ${formatearFechaHora(vigente.actualizadoEn)}`
            : ' · valor sembrado (propuesta documentada)'}
        </CardDescription>
        <Alert role="note">
          <CircleAlertIcon />
          <AlertTitle>No es un objetivo aprobado</AlertTitle>
          <AlertDescription>
            El cliente todavía no fijó la meta. Cambiarla aquí no la aprueba: el cronograma la muestra como referencia y
            nunca declara que un pedido cumple un objetivo del cliente.
          </AlertDescription>
        </Alert>
      </CardHeader>
      <CardContent className="flex flex-col gap-6">
        <form noValidate onSubmit={handleSubmit(guardar)} className="flex flex-col gap-4">
          <Field data-invalid={errors.metaDias ? true : undefined}>
            <FieldLabel htmlFor={`${ids}-meta`}>Meta propuesta (días de pedido a llegada)</FieldLabel>
            <Input
              id={`${ids}-meta`}
              inputMode="numeric"
              autoComplete="off"
              className="max-w-40 tabular-nums"
              aria-invalid={errors.metaDias ? true : undefined}
              aria-describedby={`${ids}-ayuda`}
              {...register('metaDias')}
            />
            <FieldDescription id={`${ids}-ayuda`}>
              Número entero desde 1. Días corridos de la fecha del pedido a la llegada.
            </FieldDescription>
            <FieldError>{errors.metaDias?.message}</FieldError>
          </Field>

          {conflicto && (
            <Alert variant="destructive" aria-live="polite">
              <CircleAlertIcon />
              <AlertTitle>Otra persona cambió la meta mientras editabas</AlertTitle>
              <AlertDescription className="flex w-full flex-col gap-3">
                <p>
                  Tu valor no se guardó y sigue en el formulario.{' '}
                  {releyendo || !vigenteDistinta
                    ? 'Cargando el valor vigente…'
                    : `El valor vigente es ${vigente.metaDias} días.`}
                </p>
                {/* Los botones outline heredarían el rojo atenuado de la alerta (3,38:1 sobre la tarjeta); con el color de
                    texto principal superan 4,5:1 (WCAG 1.4.3, F5-B3-H03). */}
                <div className="flex flex-wrap gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="text-foreground"
                    disabled={releyendo || !vigenteDistinta}
                    onClick={() => {
                      reset({ metaDias: String(vigente.metaDias) });
                      setVersionLeida(vigente.version);
                      setConflicto(false);
                    }}
                  >
                    Usar el valor vigente
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="text-foreground"
                    disabled={releyendo || !vigenteDistinta}
                    onClick={() => {
                      // Conserva lo escrito sobre la versión vigente; la persona vuelve a guardar si quiere.
                      setVersionLeida(vigente.version);
                      setConflicto(false);
                    }}
                  >
                    Mantener mi valor
                  </Button>
                </div>
              </AlertDescription>
            </Alert>
          )}

          {fallo !== null && (
            <Alert variant="destructive" aria-live="polite">
              <CircleAlertIcon />
              <AlertDescription>{mensajeDeError(fallo, 'No se pudo guardar la meta. Intenta de nuevo.')}</AlertDescription>
            </Alert>
          )}

          <div className="flex justify-end">
            <Button type="submit" disabled={actualizar.isPending || conflicto || !isDirty}>
              {actualizar.isPending ? (
                <LoaderIcon data-icon="inline-start" className="animate-spin" />
              ) : (
                <SaveIcon data-icon="inline-start" />
              )}
              Guardar meta
            </Button>
          </div>
        </form>

        <section aria-labelledby={`${ids}-feriados`} className="flex flex-col gap-1">
          <h3 id={`${ids}-feriados`} className="font-heading font-semibold">
            Feriados
          </h3>
          <p className="text-sm text-muted-foreground">
            Pendientes del cliente (Ecuador y China). No se configuran ni se descuentan de los plazos.
          </p>
        </section>
      </CardContent>
    </Card>
  );
}
