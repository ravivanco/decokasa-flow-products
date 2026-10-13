package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.VersionDesactualizada;
import com.decokasa.seguimiento.usuarios.application.port.in.ActualizarUsuario;
import com.decokasa.seguimiento.usuarios.application.port.out.RolRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Telefono;
import com.decokasa.seguimiento.usuarios.domain.Usuario;

/**
 * Edición de usuarios por el admin (R6). Los roles reemplazan la lista y exigen al menos uno; desactivar revoca las
 * sesiones abiertas; el usuario no se borra (requerimientos 4.7).
 */
public final class ActualizarUsuarioCasoDeUso implements ActualizarUsuario {

    private final UsuarioRepositorio usuarios;
    private final RolRepositorio roles;
    private final TokenRenovacionRepositorio tokens;
    private final Reloj reloj;
    private final Autorizacion autorizacion;

    public ActualizarUsuarioCasoDeUso(UsuarioRepositorio usuarios, RolRepositorio roles,
            TokenRenovacionRepositorio tokens, Reloj reloj) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.tokens = tokens;
        this.reloj = reloj;
        this.autorizacion = new Autorizacion(usuarios);
    }

    @Override
    public Usuario actualizar(Solicitante solicitante, Comando comando) {
        autorizacion.exigirAdmin(solicitante);
        // D10: se bloquea y relee la fila, y la versión se compara antes de cambiar nada. De dos ediciones con la
        // misma versión, la segunda espera a la primera y encuentra otra versión: 409, sin cambios ni revocaciones.
        Usuario usuario = usuarios.buscarParaActualizar(comando.usuarioId())
                .orElseThrow(() -> new NoEncontrado("El usuario no existe."));
        if (usuario.version() != comando.version()) {
            throw new VersionDesactualizada(
                    "Otra persona modificó este usuario. Vuelve a cargarlo y repite el cambio.");
        }
        ValidacionRoles.exigirExistentes(roles, comando.roles());
        Telefono telefono = comando.telefono() == null ? null : Telefono.normalizar(comando.telefono());
        if (comando.correo() != null) {
            usuarios.buscarPorCorreo(comando.correo().strip())
                    .filter(otro -> !otro.id().equals(usuario.id()))
                    .ifPresent(otro -> {
                        throw new DatoDuplicado("correo", "Ya existe un usuario con ese correo.");
                    });
        }
        if (telefono != null) {
            usuarios.buscarPorTelefono(telefono)
                    .filter(otro -> !otro.id().equals(usuario.id()))
                    .ifPresent(otro -> {
                        throw new DatoDuplicado("telefono", "Ya existe un usuario con ese teléfono.");
                    });
        }

        if (comando.roles() != null) {
            usuario.asignarRoles(comando.roles());
        }
        usuario.cambiarDatos(comando.nombre(), comando.correo(), telefono);
        if (comando.paises() != null) {
            usuario.asignarPaises(comando.paises());
        }
        if (Boolean.FALSE.equals(comando.activo())) {
            usuario.desactivar();
            tokens.revocarDeUsuario(usuario.id(), reloj.ahora());
        } else if (Boolean.TRUE.equals(comando.activo())) {
            usuario.activar();
        }
        usuarios.guardar(usuario);
        // Lo guardado, con la versión que quedó en la base.
        return usuarios.buscarPorId(usuario.id()).orElseThrow();
    }
}
