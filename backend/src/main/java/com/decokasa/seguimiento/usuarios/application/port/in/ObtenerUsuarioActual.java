package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.Set;
import java.util.UUID;

/**
 * Usuario de la sesión actual con sus permisos efectivos: la unión de los permisos de todos sus roles
 * (contrato: GET /auth/yo, UsuarioActual).
 *
 * @throws com.decokasa.seguimiento.usuarios.domain.SesionInvalida si ya no existe, está desactivado o no tiene roles
 */
public interface ObtenerUsuarioActual {

    UsuarioActual obtener(UUID usuarioId);

    record UsuarioActual(Usuario usuario, Set<String> permisos) {
    }
}
