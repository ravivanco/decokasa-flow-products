package com.decokasa.seguimiento.asistente.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Set;

/**
 * F7-D4 (aprobada por Richard el 2026-10-22, piloto): roles que dan acceso al asistente. {@code consulta} y
 * {@code asistente_china} no lo dan por sí solos, pero tampoco quitan el que dé otro rol. El rol habilita el chat, no los
 * datos: país, pedido y descarga (P-17) se comprueban aparte en cada consulta.
 */
public final class RolesAsistente {

    public static final Set<CodigoRol> HABILITADOS = Set.of(new CodigoRol("admin"),
            new CodigoRol("asistente_compras"), new CodigoRol("marketing"), new CodigoRol("compras_china"),
            new CodigoRol("logistica"));

    private RolesAsistente() {
    }

    /** Con los roles vigentes en la base, nunca con los del token (lección A1). */
    public static boolean habilita(Set<CodigoRol> rolesVigentes) {
        return rolesVigentes.stream().anyMatch(HABILITADOS::contains);
    }
}
