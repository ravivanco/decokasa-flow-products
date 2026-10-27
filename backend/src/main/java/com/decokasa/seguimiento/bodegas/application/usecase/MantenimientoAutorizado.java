package com.decokasa.seguimiento.bodegas.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.Set;

/**
 * F4-D1, provisional del equipo (2026-10-19), no confirmada por el cliente: mantienen el catálogo Logística y admin.
 * Se decide con el estado vigente en la base (cuenta activa con roles), no con los roles del JWT (lección A1). Desde la
 * fase 7 (ADR-0024), además, con el país de la bodega asignado hoy.
 */
final class MantenimientoAutorizado {

    private static final Set<CodigoRol> PUEDEN = Set.of(new CodigoRol("logistica"), new CodigoRol("admin"));

    private final ConsultarResponsables usuarios;

    MantenimientoAutorizado(ConsultarResponsables usuarios) {
        this.usuarios = usuarios;
    }

    /**
     * Rol y cuenta vigentes (403 si no), antes de leer o bloquear ninguna bodega.
     *
     * @return el estado vigente, con los países asignados hoy
     */
    ConsultarResponsables.EstadoVigente exigirRol(Solicitante solicitante) {
        ConsultarResponsables.EstadoVigente estado = solicitante == null || solicitante.usuarioId() == null
                ? null
                : usuarios.estado(solicitante.usuarioId())
                        .filter(ConsultarResponsables.EstadoVigente::activo)
                        .filter(vigente -> vigente.roles().stream().anyMatch(PUEDEN::contains))
                        .orElse(null);
        if (estado == null) {
            throw new SinPermiso("Solo Logística o el admin mantienen el catálogo de bodegas.");
        }
        return estado;
    }

    /** Alta: rol vigente y el país pedido asignado (403 si no; el país lo eligió quien crea). */
    void exigir(Solicitante solicitante, CodigoPais pais) {
        if (!exigirRol(solicitante).paises().contains(pais)) {
            throw new SinPermiso("No tienes asignado el país de esta bodega.");
        }
    }
}
