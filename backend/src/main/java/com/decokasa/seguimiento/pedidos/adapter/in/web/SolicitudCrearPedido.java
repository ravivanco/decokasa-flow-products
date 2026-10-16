package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Esquema SolicitudCrearPedido del contrato. El país por defecto es EC (ADR-0009). */
public record SolicitudCrearPedido(@NotNull TipoFlujo tipoFlujo, CodigoPais pais,
        @NotBlank @Size(max = 200) String nombre, @Size(max = 4000) String descripcion,
        Boolean incluyeProductosNuevos) {
}
