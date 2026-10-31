import { LinkIcon, PencilIcon, PlusIcon } from 'lucide-react';
import { useState } from 'react';
import { toast } from 'sonner';
import { esErrorApi } from '@/api/cliente';
import { destinoGoogleValido } from '@/lib/destinoGoogle';
import { irA } from '@/lib/navegacion';
import {
  useActualizarUsuario,
  useConectarGoogle,
  useRoles,
  useUsuarios,
} from '@/api/consultas';
import type { Usuario } from '@/api/esquemas';
import { mensajeDeError } from '@/api/mensajes';
import { esPais, NOMBRE_PAIS } from '@/pais/PaisProvider';
import { useUsuarioActual } from '@/auth/SesionProvider';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { FormularioUsuario } from './usuarios/FormularioUsuario';

// Gestión de usuarios del admin (plan, tarea 22): lista, alta, edición de datos y roles, activar y desactivar. Los
// permisos los valida el servidor con el estado actual del solicitante (A1); esta pantalla solo se muestra al admin.

type Edicion = { tipo: 'alta' } | { tipo: 'edicion'; usuario: Usuario } | null;

export function UsersManagementView() {
  const yo = useUsuarioActual();
  const usuarios = useUsuarios();
  const roles = useRoles();
  const actualizar = useActualizarUsuario();
  // «Conectar Google Drive» (recuperado el 2026-10-31): en Render la cuenta central se autoriza desde aquí. El servidor
  // revalida al admin; el navegador solo sale hacia Google o hacia la salida de un solo uso del backend.
  const conectarGoogle = useConectarGoogle();

  function conectar() {
    setAviso(null);
    conectarGoogle.mutate(undefined, {
      onSuccess: ({ urlAutorizacion }) => {
        if (destinoGoogleValido(urlAutorizacion)) irA(urlAutorizacion);
        else setAviso('El servidor devolvió un destino inesperado para la conexión con Google. No se abrió.');
      },
      onError: (error) =>
        setAviso(
          esErrorApi(error) && error.status === 404
            ? 'La conexión con Google no está habilitada en este servidor.'
            : mensajeDeError(error, 'No se pudo iniciar la conexión con Google. Intenta de nuevo.'),
        ),
    });
  }

  const [edicion, setEdicion] = useState<Edicion>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  const nombreDeRol = (codigo: string) => roles.data?.find((rol) => rol.codigo === codigo)?.nombre ?? codigo;

  async function recargar(id: string) {
    const resultado = await usuarios.refetch({ throwOnError: true });
    return resultado.data?.find((usuario) => usuario.id === id);
  }

  function cambiarEstado(usuario: Usuario) {
    setAviso(null);
    actualizar.mutate(
      { id: usuario.id, version: usuario.version, activo: !usuario.activo },
      {
        onSuccess: (guardado) =>
          toast.success(`${guardado.nombre} quedó ${guardado.activo ? 'activo' : 'inactivo'}.`),
        onError: (error) =>
          setAviso(
            esErrorApi(error, 'VERSION_DESACTUALIZADA')
              ? `La lista estaba desactualizada: otra persona modificó a ${usuario.nombre}. Ya se muestran los datos actuales; revísalos y vuelve a intentarlo si todavía hace falta.`
              : mensajeDeError(error, `No se pudo cambiar el estado de ${usuario.nombre}. Intenta de nuevo.`),
          ),
      },
    );
  }

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="font-heading text-2xl font-bold text-primary">Usuarios</h1>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={conectar} disabled={conectarGoogle.isPending}>
            <LinkIcon data-icon="inline-start" />
            Conectar Google Drive
          </Button>
          <Button onClick={() => setEdicion({ tipo: 'alta' })} disabled={!roles.data}>
            <PlusIcon data-icon="inline-start" />
            Nuevo usuario
          </Button>
        </div>
      </div>

      {aviso && (
        <Alert variant="destructive">
          <AlertDescription>{aviso}</AlertDescription>
        </Alert>
      )}

      {usuarios.isPending && (
        <p role="status" className="text-muted-foreground">
          Cargando usuarios…
        </p>
      )}
      {usuarios.isError && (
        <Alert variant="destructive">
          <AlertDescription>{mensajeDeError(usuarios.error, 'No se pudieron cargar los usuarios.')}</AlertDescription>
        </Alert>
      )}

      {usuarios.data && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nombre</TableHead>
              <TableHead>Correo</TableHead>
              <TableHead>Usuario</TableHead>
              <TableHead>Teléfono</TableHead>
              <TableHead>Roles</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead>Países</TableHead>
              <TableHead>
                <span className="sr-only">Acciones</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {usuarios.data.map((usuario) => (
              <TableRow key={usuario.id}>
                <TableCell className="font-medium">{usuario.nombre}</TableCell>
                <TableCell>{usuario.correo}</TableCell>
                <TableCell>{usuario.usuario}</TableCell>
                <TableCell>{usuario.telefono}</TableCell>
                <TableCell>
                  <div className="flex flex-wrap gap-1">
                    {usuario.roles.map((codigo) => (
                      <Badge key={codigo} variant="secondary">
                        {nombreDeRol(codigo)}
                      </Badge>
                    ))}
                  </div>
                </TableCell>
                <TableCell>
                  <Badge variant={usuario.activo ? 'default' : 'outline'}>
                    {usuario.activo ? 'Activo' : 'Inactivo'}
                  </Badge>
                </TableCell>
                <TableCell>
                  {usuario.paises.length > 0
                    ? usuario.paises.map((pais) => (esPais(pais) ? NOMBRE_PAIS[pais] : pais)).join(', ')
                    : 'Sin país'}
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      aria-label={`Editar a ${usuario.nombre}`}
                      disabled={!roles.data}
                      onClick={() => setEdicion({ tipo: 'edicion', usuario })}
                    >
                      <PencilIcon data-icon="inline-start" />
                      Editar
                    </Button>
                    {usuario.id !== yo.id && (
                      <Button
                        variant={usuario.activo ? 'destructive' : 'secondary'}
                        size="sm"
                        disabled={actualizar.isPending}
                        aria-label={`${usuario.activo ? 'Desactivar' : 'Activar'} a ${usuario.nombre}`}
                        onClick={() => cambiarEstado(usuario)}
                      >
                        {usuario.activo ? 'Desactivar' : 'Activar'}
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      {edicion && roles.data && (
        <FormularioUsuario
          usuario={edicion.tipo === 'edicion' ? edicion.usuario : null}
          roles={roles.data}
          recargar={recargar}
          onCerrar={() => setEdicion(null)}
        />
      )}
    </section>
  );
}
