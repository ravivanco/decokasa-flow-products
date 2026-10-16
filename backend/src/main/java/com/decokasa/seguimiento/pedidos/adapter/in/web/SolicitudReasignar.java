package com.decokasa.seguimiento.pedidos.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Cuerpo de PUT /pedidos/{id}/etapas/{n}/asignacion del contrato. */
public record SolicitudReasignar(@NotNull UUID usuarioId, @Size(max = 1000) String motivo) {
}
