import { EyeIcon, EyeOffIcon } from 'lucide-react';
import { useState } from 'react';
import type { FieldError as ErrorDeCampo, UseFormRegisterReturn } from 'react-hook-form';
import { Button } from '@/components/ui/button';
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';

// Campo de contraseña con «Mostrar/Ocultar» (ADR-0033): botón real dentro del campo, con nombre accesible propio,
// aria-pressed y aria-controls. Mostrarla no cambia el valor ni el autocompletado.
export function CampoContrasena({
  id,
  etiqueta,
  autoComplete,
  registro,
  error,
  ayuda,
}: {
  id: string;
  etiqueta: string;
  autoComplete: 'current-password' | 'new-password';
  registro: UseFormRegisterReturn;
  error?: ErrorDeCampo;
  ayuda?: string;
}) {
  const [visible, setVisible] = useState(false);
  const accion = `${visible ? 'Ocultar' : 'Mostrar'} ${etiqueta.toLowerCase()}`;
  return (
    <Field data-invalid={error ? true : undefined}>
      <FieldLabel htmlFor={id}>{etiqueta}</FieldLabel>
      <div className="relative">
        <Input
          id={id}
          type={visible ? 'text' : 'password'}
          autoComplete={autoComplete}
          spellCheck={false}
          autoCapitalize="none"
          className="pr-11"
          aria-invalid={error ? true : undefined}
          aria-describedby={ayuda ? `${id}-ayuda` : undefined}
          {...registro}
        />
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="absolute top-0 right-0 text-muted-foreground hover:text-foreground"
          aria-label={accion}
          title={accion}
          aria-pressed={visible}
          aria-controls={id}
          onClick={() => setVisible((v) => !v)}
        >
          {visible ? <EyeOffIcon aria-hidden="true" /> : <EyeIcon aria-hidden="true" />}
        </Button>
      </div>
      {ayuda && <FieldDescription id={`${id}-ayuda`}>{ayuda}</FieldDescription>}
      <FieldError errors={[error]} />
    </Field>
  );
}
