package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Estado actual de una persona en la base: si su cuenta está activa, qué roles tiene hoy y a qué países tiene acceso.
 * El caso de uso (paso 2.4) lo arma con el puerto ConsultarResponsables de {@code usuarios}, dentro de la transacción
 * de la acción; nunca con los roles del token (lección A1).
 *
 * <p>Fase 7 (ADR-0024): un pedido de un país que la persona no tiene asignado no existe para ella, tampoco para el
 * admin.
 *
 * @param activo la cuenta está activa y tiene al menos un rol
 * @param paises países asignados hoy en la base (usuario_pais)
 */
public record ActorVigente(UUID usuarioId, boolean activo, Set<CodigoRol> roles, Set<CodigoPais> paises) {

    public ActorVigente {
        Objects.requireNonNull(usuarioId, "usuarioId");
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        paises = paises == null ? Set.of() : Set.copyOf(paises);
    }

    /**
     * Solo compatibilidad documentada con código y pruebas anteriores a la fase 7 (ADR-0024): equivale a una persona con
     * Ecuador asignado. No es una autorización: en producción el actor siempre se arma con los países leídos de la
     * base ({@code ApoyoPedidos.persona}), nunca con este constructor.
     */
    public ActorVigente(UUID usuarioId, boolean activo, Set<CodigoRol> roles) {
        this(usuarioId, activo, roles, Set.of(CodigoPais.EC));
    }

    public boolean puedeActuar() {
        return activo && !roles.isEmpty();
    }

    public boolean tieneRol(CodigoRol rol) {
        return rol != null && roles.contains(rol);
    }

    /** Tiene asignado ese país hoy (ADR-0024). */
    public boolean veePais(CodigoPais pais) {
        return pais != null && paises.contains(pais);
    }
}
