package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Esquema SolicitudAccion del contrato (discriminador {@code accion}). Aquí se validan la forma y los límites; los
 * requisitos de cada acción (observación, urgencia, documentos) los decide el dominio según la definición del flujo y
 * responden 422 con {@code faltantes}. {@code nuevaFechaEstimadaLlegada} (fecha ISO, sin hora) solo va con
 * REPORTAR_DEMORA (F4-D5). El formato es estricto, {@code AAAA-MM-DD}: una fecha con hora, un número, un arreglo o un
 * texto que no es una fecha responden 400. {@code bodegaId} solo va con el APROBAR de Bodega (F4-D2); ELEGIR_BODEGA
 * no se usa.
 */
public record SolicitudAccion(@NotNull AccionEtapa accion, @NotNull @Min(1) Integer etapaEsperada,
        @Size(max = 4000) String observacion, NivelUrgencia nivelUrgencia, List<@NotNull UUID> adjuntoIds,
        @JsonDeserialize(using = FechaEstimadaLlegadaDeserializer.class)
        LocalDate nuevaFechaEstimadaLlegada, UUID bodegaId) {
}
