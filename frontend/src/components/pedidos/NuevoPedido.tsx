import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeftIcon, CircleAlertIcon } from 'lucide-react';
import { useId, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import { z } from 'zod';
import { esErrorApi } from '@/api/cliente';
import { type DefinicionFlujo, useCrearPedido, useDefinicionesVigentes } from '@/api/pedidos';
import { useUsuarioActual } from '@/auth/SesionProvider';
import { SinPermiso } from '@/components/PaginasSimples';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Skeleton } from '@/components/ui/skeleton';
import { Textarea } from '@/components/ui/textarea';
import { NOMBRE_PAIS, useCambiosSinGuardar, usePaisActivo } from '@/pais/PaisProvider';
import { codigoEtapa, flujosQuePuedeCrear, mensajeDeErrorPedido, nombreFlujo } from './formato';

// Crear un pedido (requisito 4.2 #4; fase 6, paso 6.5). Crea quien tiene el rol de la primera etapa del flujo según su
// definición vigente: el admin el flujo A ("Nuevo pedido") y Compras China el B ("Crear pedido propuesto"). El admin sin
// Compras China no crea B mientras siga pendiente (P-14). Con roles de los dos flujos se elige uno, sin valor por
// defecto. La pantalla solo lo anticipa: el servidor lo decide. El número lo genera el servidor y el pedido empieza en
// la primera etapa; los documentos se adjuntan después en la etapa y se envían con ENVIAR. Los textos salen de la
// definición del flujo, no de nombres de etapa fijos.

type Tipo = 'A' | 'B';

const TITULO: Record<Tipo, string> = { A: 'Nuevo pedido', B: 'Crear pedido propuesto' };

const esquema = z.object({
  nombre: z.string().trim().min(1, 'Escribe el nombre del pedido.').max(200, 'Hasta 200 caracteres.'),
  descripcion: z.string().max(4000, 'Hasta 4000 caracteres.'),
  incluyeProductosNuevos: z.boolean(),
});
type Datos = z.infer<typeof esquema>;

export function NuevoPedido() {
  const usuario = useUsuarioActual();
  const definiciones = useDefinicionesVigentes();
  const [parametros] = useSearchParams();

  if (definiciones.cargando) {
    return (
      <div className="flex max-w-2xl flex-col gap-3" aria-busy="true" aria-label="Cargando los flujos">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }
  const creables = flujosQuePuedeCrear(definiciones, usuario.roles);
  if (creables.length === 0) {
    return definiciones.error ? (
      <Alert variant="destructive" className="max-w-2xl">
        <CircleAlertIcon />
        <AlertDescription>No se pudieron cargar los flujos. Vuelve a intentarlo en un momento.</AlertDescription>
      </Alert>
    ) : (
      <SinPermiso />
    );
  }
  const pedido = parametros.get('flujo');
  const inicial = creables.find((tipo) => tipo === pedido) ?? (creables.length === 1 ? creables[0] : undefined);
  return <FormularioNuevoPedido creables={creables} inicial={inicial} definiciones={definiciones} />;
}

/** Primera etapa del flujo, su envío y lo que pide (todo de la definición). */
function inicioDelFlujo(definicion: DefinicionFlujo | undefined) {
  const primera = definicion?.etapas[0];
  const envio = primera?.acciones.find((accion) => accion.accion === 'ENVIAR');
  const destino = definicion?.etapas.find((etapa) => etapa.numero === envio?.etapaDestino);
  return { primera, destino, documentos: envio?.documentosObligatorios ?? [] };
}

function FormularioNuevoPedido({
  creables,
  inicial,
  definiciones,
}: {
  creables: Tipo[];
  inicial: Tipo | undefined;
  definiciones: { A?: DefinicionFlujo; B?: DefinicionFlujo };
}) {
  const ids = useId();
  const navegar = useNavigate();
  const crear = useCrearPedido();
  const pais = usePaisActivo();
  const [tipo, setTipo] = useState<Tipo | undefined>(inicial);
  const [errorTipo, setErrorTipo] = useState<string | null>(null);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const {
    register,
    control,
    handleSubmit,
    setError,
    watch,
    formState: { errors, isDirty },
  } = useForm<Datos>({
    resolver: zodResolver(esquema),
    defaultValues: { nombre: '', descripcion: '', incluyeProductosNuevos: false },
  });
  const descripcion = watch('descripcion');
  // Paso 7.3: el pedido nuevo es del país activo; cambiar de país con datos escritos pide confirmación.
  useCambiosSinGuardar(isDirty && !crear.isSuccess ? 'un pedido nuevo sin crear' : null);
  const inicio = inicioDelFlujo(tipo ? definiciones[tipo] : undefined);

  function guardar(datos: Datos) {
    setErrorGeneral(null);
    if (!tipo) {
      setErrorTipo('Elige qué pedido vas a crear.');
      return;
    }
    crear.mutate(
      {
        tipoFlujo: tipo,
        nombre: datos.nombre.trim(),
        descripcion: datos.descripcion.trim() || undefined,
        // Solo el flujo A mezcla productos existentes y nuevos (requisito 4.2 #6).
        incluyeProductosNuevos: tipo === 'A' && datos.incluyeProductosNuevos,
      },
      {
        onSuccess: (pedido) => {
          const siguiente = inicio.destino
            ? ` y envíalo a ${codigoEtapa(pedido.tipoFlujo, inicio.destino.numero)} · ${inicio.destino.nombre}`
            : '';
          const adjuntar = inicio.documentos.length > 0 ? `Adjunta ${inicio.documentos.join(', ')}${siguiente}.` : '';
          toast.success(
            `Pedido ${pedido.numero} creado en ${codigoEtapa(pedido.tipoFlujo, pedido.etapaActual)}. ${adjuntar}`.trim(),
          );
          navegar(`/pedidos/${pedido.id}`);
        },
        onError: (error) => {
          if (esErrorApi(error, 'VALIDACION')) {
            for (const { campo, mensaje } of error.errores) {
              if (campo === 'nombre' || campo === 'descripcion') setError(campo, { message: mensaje });
            }
          }
          setErrorGeneral(mensajeDeErrorPedido(error, 'No se pudo crear el pedido. Intenta de nuevo.'));
        },
      },
    );
  }

  return (
    <section className="flex max-w-2xl flex-col gap-4">
      <Button variant="ghost" asChild className="w-fit">
        <Link to="/pedidos">
          <ArrowLeftIcon data-icon="inline-start" />
          Pedidos
        </Link>
      </Button>
      <Card>
        <CardHeader>
          <CardTitle>
            <h1 className="font-heading text-2xl font-bold text-primary">{TITULO[tipo ?? 'A']}</h1>
          </CardTitle>
          {tipo && inicio.primera && (
            <CardDescription className="flex flex-col gap-1">
              <span>
                Flujo {nombreFlujo(tipo)} de {NOMBRE_PAIS[pais]}. El número se genera al guardar y el pedido empieza
                en{' '}
                {codigoEtapa(tipo, inicio.primera.numero)} · {inicio.primera.nombre}.
              </span>
              {inicio.destino && inicio.documentos.length > 0 && (
                <span>
                  Para enviarlo a {codigoEtapa(tipo, inicio.destino.numero)} · {inicio.destino.nombre} adjunta en la
                  etapa: {inicio.documentos.join(', ')}. Puedes adjuntar otros documentos
                  {tipo === 'B' ? ', como el producto propuesto' : ''}.
                </span>
              )}
            </CardDescription>
          )}
        </CardHeader>
        <CardContent>
          <form noValidate onSubmit={handleSubmit(guardar)} className="flex flex-col gap-6">
            {errorGeneral && (
              <Alert variant="destructive">
                <CircleAlertIcon />
                <AlertDescription>{errorGeneral}</AlertDescription>
              </Alert>
            )}
            <FieldGroup>
              {creables.length > 1 && (
                <FieldSet data-invalid={errorTipo ? true : undefined}>
                  <FieldLegend variant="label">Qué pedido vas a crear</FieldLegend>
                  <FieldDescription>Tus roles permiten crear los dos tipos. El servidor vuelve a comprobarlo.</FieldDescription>
                  <RadioGroup
                    value={tipo ?? ''}
                    onValueChange={(valor) => {
                      setTipo(valor as Tipo);
                      setErrorTipo(null);
                    }}
                    disabled={crear.isPending}
                    aria-invalid={errorTipo ? true : undefined}
                    className="grid gap-2 sm:grid-cols-2"
                  >
                    {creables.map((opcion) => (
                      <Field key={opcion} orientation="horizontal" className="min-h-10">
                        <RadioGroupItem value={opcion} id={`${ids}-${opcion}`} />
                        <FieldLabel htmlFor={`${ids}-${opcion}`} className="font-normal">
                          {TITULO[opcion]} · {nombreFlujo(opcion)}
                        </FieldLabel>
                      </Field>
                    ))}
                  </RadioGroup>
                  <FieldError>{errorTipo}</FieldError>
                </FieldSet>
              )}
              <Field data-invalid={errors.nombre ? true : undefined}>
                <FieldLabel htmlFor="pedido-nombre">Nombre del pedido</FieldLabel>
                <Input
                  id="pedido-nombre"
                  autoComplete="off"
                  maxLength={200}
                  aria-invalid={errors.nombre ? true : undefined}
                  {...register('nombre')}
                />
                <FieldError errors={[errors.nombre]} />
              </Field>
              <Field data-invalid={errors.descripcion ? true : undefined}>
                <FieldLabel htmlFor="pedido-descripcion">Descripción (opcional)</FieldLabel>
                <Textarea
                  id="pedido-descripcion"
                  maxLength={4000}
                  aria-invalid={errors.descripcion ? true : undefined}
                  aria-describedby="pedido-descripcion-contador"
                  {...register('descripcion')}
                />
                <FieldDescription id="pedido-descripcion-contador" className="tabular-nums">
                  {descripcion.length} de 4000 caracteres
                </FieldDescription>
                <FieldError errors={[errors.descripcion]} />
              </Field>
              {tipo === 'A' && (
                <Field orientation="horizontal">
                  <Controller
                    control={control}
                    name="incluyeProductosNuevos"
                    render={({ field }) => (
                      <Checkbox
                        id="pedido-nuevos"
                        checked={field.value}
                        onCheckedChange={(marcado) => field.onChange(marcado === true)}
                      />
                    )}
                  />
                  <FieldLabel htmlFor="pedido-nuevos" className="font-normal">
                    Incluye productos nuevos además de los existentes
                  </FieldLabel>
                </Field>
              )}
            </FieldGroup>
            <div className="flex flex-wrap justify-end gap-2">
              <Button variant="outline" asChild>
                <Link to="/pedidos">Cancelar</Link>
              </Button>
              <Button type="submit" disabled={crear.isPending}>
                {crear.isPending ? 'Creando…' : tipo === 'B' ? 'Crear pedido propuesto' : 'Crear pedido'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </section>
  );
}
