package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Una fila de usuario_rol: el rol asignado y cuándo se asignó (modelo-datos.md 3.1). */
@Embeddable
public class AsignacionRolEntity {

    @Column(name = "rol_id", nullable = false)
    private UUID rolId;

    @Column(name = "asignado_en", nullable = false)
    private Instant asignadoEn;

    protected AsignacionRolEntity() {
    }

    AsignacionRolEntity(UUID rolId, Instant asignadoEn) {
        this.rolId = rolId;
        this.asignadoEn = asignadoEn;
    }

    UUID rolId() {
        return rolId;
    }

    Instant asignadoEn() {
        return asignadoEn;
    }

    // Igualdad por rol: un mismo rol conserva su fecha de asignación cuando se reemplaza la lista.
    @Override
    public boolean equals(Object otro) {
        return otro instanceof AsignacionRolEntity asignacion && Objects.equals(rolId, asignacion.rolId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rolId);
    }
}
