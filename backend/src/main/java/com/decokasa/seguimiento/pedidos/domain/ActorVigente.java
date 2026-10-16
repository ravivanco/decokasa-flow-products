package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Estado actual de una persona en la base: si su cuenta está activa y qué roles tiene hoy. El caso de uso (paso 2.4)
 * lo arma con el puerto ConsultarResponsables de {@code usuarios}, dentro de la transacción de la acción; nunca con
 * los roles del token (lección A1).
 *
 * @param activo la cuenta está activa y tiene al menos un rol
 */
public record ActorVigente(UUID usuarioId, boolean activo, Set<CodigoRol> roles) {

    public ActorVigente {
        Objects.requireNonNull(usuarioId, "usuarioId");
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean puedeActuar() {
        return activo && !roles.isEmpty();
    }

    public boolean tieneRol(CodigoRol rol) {
        return rol != null && roles.contains(rol);
    }
}
