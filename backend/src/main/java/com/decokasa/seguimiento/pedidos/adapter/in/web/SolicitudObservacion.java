package com.decokasa.seguimiento.pedidos.adapter.in.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Cuerpo de POST /pedidos/{id}/observaciones del contrato. */
public record SolicitudObservacion(@NotBlank @Size(max = 4000) String texto, @NotNull @Min(1) Integer etapaEsperada,
        @NotNull UUID entradaEsperadaId, List<@NotNull UUID> adjuntoIds) {
}
