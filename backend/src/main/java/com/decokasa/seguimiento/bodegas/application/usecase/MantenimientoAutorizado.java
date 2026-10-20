package com.decokasa.seguimiento.bodegas.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.Set;

/**
 * F4-D1, provisional del equipo (2026-10-19), no confirmada por el cliente: mantienen el catálogo Logística y admin.
 * Se decide con el estado vigente en la base (cuenta activa con roles), no con los roles del JWT (lección A1).
 */
final class MantenimientoAutorizado {

    private static final Set<CodigoRol> PUEDEN = Set.of(new CodigoRol("logistica"), new CodigoRol("admin"));

    private final ConsultarResponsables usuarios;

    MantenimientoAutorizado(ConsultarResponsables usuarios) {
        this.usuarios = usuarios;
    }

    void exigir(Solicitante solicitante) {
        boolean puede = solicitante != null && solicitante.usuarioId() != null
                && usuarios.estado(solicitante.usuarioId())
                        .filter(ConsultarResponsables.EstadoVigente::activo)
                        .filter(estado -> estado.roles().stream().anyMatch(PUEDEN::contains))
                        .isPresent();
        if (!puede) {
            throw new SinPermiso("Solo Logística o el admin mantienen el catálogo de bodegas.");
        }
    }
}
