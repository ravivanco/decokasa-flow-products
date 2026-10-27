package com.decokasa.seguimiento.asistente.adapter.out.modulos;

import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.Optional;
import java.util.UUID;

/** Estado vigente desde el puerto de entrada de usuarios (R4): activo, roles y países de la base. */
public final class CuentasParaAsistenteAdaptador implements CuentasParaAsistente {

    private final ConsultarResponsables responsables;

    public CuentasParaAsistenteAdaptador(ConsultarResponsables responsables) {
        this.responsables = responsables;
    }

    @Override
    public Optional<EstadoCuenta> estado(UUID usuarioId) {
        return responsables.estado(usuarioId)
                .map(estado -> new EstadoCuenta(estado.activo(), estado.roles(), estado.paises()));
    }
}
