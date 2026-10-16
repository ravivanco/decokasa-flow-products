package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Esquema SolicitudAccion del contrato (discriminador {@code accion}). Aquí se validan la forma y los límites; los
 * requisitos de cada acción (observación, urgencia, documentos) los decide el dominio según la definición del flujo y
 * responden 422 con {@code faltantes}. REPORTAR_DEMORA y ELEGIR_BODEGA no están habilitadas en la fase 2.
 */
public record SolicitudAccion(@NotNull AccionEtapa accion, @NotNull @Min(1) Integer etapaEsperada,
        @Size(max = 4000) String observacion, NivelUrgencia nivelUrgencia, List<@NotNull UUID> adjuntoIds) {
}
