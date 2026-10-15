package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.Set;
import java.util.UUID;

/**
 * Cambia datos, roles, países o el estado activo de un usuario; solo el admin. Un campo nulo no cambia. Los roles
 * reemplazan la lista completa y exigen al menos uno; desactivar revoca las sesiones abiertas (requerimientos 4.7).
 *
 * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si el solicitante no es admin
 * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si el usuario no existe
 * @throws com.decokasa.seguimiento.compartido.domain.DatoInvalido con una lista de roles vacía
 * @throws com.decokasa.seguimiento.compartido.domain.DatoDuplicado correo o teléfono de otro usuario
 * @throws com.decokasa.seguimiento.compartido.domain.VersionDesactualizada si la versión leída ya no es la vigente
 *         (D10); se comprueba antes que los duplicados y no se aplica ningún cambio
 */
public interface ActualizarUsuario {

    Usuario actualizar(Solicitante solicitante, Comando comando);

    /** {@code version}: la que leyó el cliente (D10); si ya no es la vigente, nada cambia. */
    record Comando(UUID usuarioId, String nombre, String correo, String telefono, Set<CodigoRol> roles,
            Set<CodigoPais> paises, Boolean activo, long version) {
    }
}
