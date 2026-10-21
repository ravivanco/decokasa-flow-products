package com.decokasa.seguimiento.usuarios.application.port.out;

import com.decokasa.seguimiento.usuarios.domain.Telefono;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.List;
import java.util.Set;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Optional;
import java.util.UUID;

/** Usuarios guardados. El correo y el nombre de usuario se buscan sin distinguir mayúsculas. */
public interface UsuarioRepositorio {

    Optional<Usuario> buscarPorId(UUID id);

    /**
     * Relee el usuario tal como está confirmado y bloquea su fila hasta el final de la transacción. Las solicitudes
     * simultáneas sobre la misma cuenta esperan su turno y ven lo que guardó la anterior (hallazgo A4: cada intento
     * fallido se cuenta y el bloqueo se respeta).
     */
    Optional<Usuario> buscarParaActualizar(UUID id);

    Optional<Usuario> buscarPorCorreo(String correo);

    Optional<Usuario> buscarPorNombreDeUsuario(String usuario);

    Optional<Usuario> buscarPorTelefono(Telefono telefono);

    List<Usuario> listar();

    /** Proyección mínima para pedidos/archivos; no carga países ni credenciales. */
    record Resumen(UUID id, String nombre, Set<CodigoRol> roles, boolean activo) {
        public Resumen { roles = Set.copyOf(roles); }
    }

    /**
     * Solo IDs solicitados. El adaptador persistente lo resuelve por lote en una sentencia.
     * La implementación básica mantiene compatibles los repositorios en memoria.
     */
    default List<Resumen> resumirPorIds(Set<UUID> ids) {
        return ids.stream().map(this::buscarPorId).flatMap(Optional::stream)
                .map(u -> new Resumen(u.id(), u.nombre(), u.roles(), u.puedeIniciarSesion())).toList();
    }

    void guardar(Usuario usuario);

    /**
     * Guarda solo el contador de intentos fallidos y el fin del bloqueo, sin cambiar la versión administrativa del
     * usuario (D10). Lo usa el inicio de sesión, con la fila ya bloqueada ({@link #buscarParaActualizar}).
     */
    void guardarIntentos(Usuario usuario);
}
