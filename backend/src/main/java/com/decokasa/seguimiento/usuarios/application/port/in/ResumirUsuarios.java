package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Nombre, roles y estado actuales de varias personas, para mostrarlas en pedidos y archivos (contrato:
 * UsuarioResumen) y saber si una asignación sigue vigente. Sin correo, teléfono ni credenciales. Lo usan otros
 * módulos sin tocar el dominio de usuarios (R4).
 */
public interface ResumirUsuarios {

    /** Las personas que existen; las que no, se omiten. */
    Map<UUID, Resumen> resumenes(Collection<UUID> usuarioIds);

    /** @param activo la cuenta está activa y tiene al menos un rol */
    record Resumen(UUID id, String nombre, Set<CodigoRol> roles, boolean activo) {

        public Resumen {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(nombre, "nombre");
            roles = Set.copyOf(roles);
        }
    }
}
