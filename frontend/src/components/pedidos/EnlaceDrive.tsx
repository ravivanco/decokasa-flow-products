import { useState } from 'react';
import { z } from 'zod';
import { ExternalLinkIcon } from 'lucide-react';
import { useSesion } from '@/auth/SesionProvider';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { mensajeDeErrorPedido } from './formato';

const esquema = z.object({ url: z.string().url().refine((valor) => {
  const u = new URL(valor);
  return u.protocol === 'https:' && u.hostname === 'drive.google.com' && !u.port && !u.username && !u.password;
}) });

/** Se consulta bajo demanda y con permiso vigente. No se fabrican URLs ni se consulta Google en el navegador. */
export function EnlaceDrive({ id }: { id: string }) {
  const { pedir } = useSesion();
  const [ocupado, setOcupado] = useState(false);
  const [url, setUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  async function consultar() {
    setOcupado(true); setError(null); setUrl(null);
    try {
      const enlace = await pedir(`/archivos/${encodeURIComponent(id)}/enlace`, { esquema });
      setUrl(enlace.url);
    } catch (fallo) { setError(mensajeDeErrorPedido(fallo, 'No hay enlace confirmado o no tienes permiso.')); }
    finally { setOcupado(false); }
  }
  return <div className="flex flex-col gap-2">
    {url ? <Button asChild variant="outline" size="sm"><a href={url} target="_blank" rel="noopener noreferrer">
      <ExternalLinkIcon data-icon="inline-start" />Abrir en Drive</a></Button>
      : <Button variant="outline" size="sm" disabled={ocupado} onClick={() => void consultar()}>
        {ocupado ? 'Consultando enlace…' : 'Consultar enlace de Drive'}</Button>}
    {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
  </div>;
}
