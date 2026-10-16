import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeftIcon } from 'lucide-react';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { toast } from 'sonner';
import { z } from 'zod';
import { esErrorApi } from '@/api/cliente';
import { useCrearPedido } from '@/api/pedidos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { mensajeDeErrorPedido } from './formato';

// Crear un pedido del flujo A (requisito 4.2 #4: lo crea el admin; la ruta está bajo SoloAdmin y el servidor lo
// vuelve a comprobar). El servidor genera el número y deja el pedido en A1; la solicitud se adjunta después en la
// etapa y se envía con la acción ENVIAR. El flujo B no se ofrece: en la fase 2 no tiene definición sembrada y quién
// lo crea sigue pendiente del cliente (P-14). Los límites son los del contrato.

const esquema = z.object({
  nombre: z.string().trim().min(1, 'Escribe el nombre del pedido.').max(200, 'Hasta 200 caracteres.'),
  descripcion: z.string().max(4000, 'Hasta 4000 caracteres.'),
  incluyeProductosNuevos: z.boolean(),
});
type Datos = z.infer<typeof esquema>;

export function NuevoPedido() {
  const navegar = useNavigate();
  const crear = useCrearPedido();
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const {
    register,
    control,
    handleSubmit,
    setError,
    watch,
    formState: { errors },
  } = useForm<Datos>({
    resolver: zodResolver(esquema),
    defaultValues: { nombre: '', descripcion: '', incluyeProductosNuevos: false },
  });
  const descripcion = watch('descripcion');

  function guardar(datos: Datos) {
    setErrorGeneral(null);
    crear.mutate(
      {
        tipoFlujo: 'A',
        nombre: datos.nombre.trim(),
        descripcion: datos.descripcion.trim() || undefined,
        incluyeProductosNuevos: datos.incluyeProductosNuevos,
      },
      {
        onSuccess: (pedido) => {
          toast.success(`Pedido ${pedido.numero} creado. Adjunta la solicitud y envíalo a Codificación.`);
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
            <h1 className="font-heading text-2xl font-bold text-primary">Nuevo pedido</h1>
          </CardTitle>
          <CardDescription>
            Flujo A · Ecuador. El número se genera al guardar y el pedido empieza en A1, Crear solicitud de pedido.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form noValidate onSubmit={handleSubmit(guardar)} className="flex flex-col gap-6">
            {errorGeneral && (
              <Alert variant="destructive">
                <AlertDescription>{errorGeneral}</AlertDescription>
              </Alert>
            )}
            <FieldGroup>
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
            </FieldGroup>
            <div className="flex flex-wrap justify-end gap-2">
              <Button variant="outline" asChild>
                <Link to="/pedidos">Cancelar</Link>
              </Button>
              <Button type="submit" disabled={crear.isPending}>
                {crear.isPending ? 'Creando…' : 'Crear pedido'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </section>
  );
}
