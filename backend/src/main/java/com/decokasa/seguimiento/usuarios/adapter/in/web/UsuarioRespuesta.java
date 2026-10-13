package com.decokasa.seguimiento.usuarios.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.List;
import java.util.UUID;

/**
 * Esquema Usuario del contrato: sin el hash de la contraseña ni los datos de bloqueo, con la versión administrativa
 * (D10) que el cliente devuelve al editar.
 */
public record UsuarioRespuesta(UUID id, String nombre, String correo, String usuario, String telefono,
        List<String> roles, List<String> paises, boolean activo, long version) {

    static UsuarioRespuesta de(Usuario usuario) {
        return new UsuarioRespuesta(usuario.id(), usuario.nombre(), usuario.correo(), usuario.usuario(),
                usuario.telefono().valor(), usuario.roles().stream().map(CodigoRol::valor).sorted().toList(),
                usuario.paises().stream().map(CodigoPais::name).sorted().toList(), usuario.activo(),
                usuario.version());
    }
}
