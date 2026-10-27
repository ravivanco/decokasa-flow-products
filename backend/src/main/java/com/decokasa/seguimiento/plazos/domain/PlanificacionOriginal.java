package com.decokasa.seguimiento.plazos.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Planificación original de un pedido (ADR-0030): las fechas previstas al crearlo, solo con su flujo, su versión y la
 * hora de creación. Es la referencia histórica con la que se compara la estimación actual. Java puro (R1).
 *
 * @param origen      GUARDADA si se guardó al crear el pedido (V25); RECONSTRUIDA si el pedido es anterior y se
 *                    calcula al consultar con las reglas vigentes
 * @param reglas      versión de las reglas de estimación
 * @param calculadaEn cuándo se calculó: la creación del pedido o el momento de la consulta
 * @param llegada     llegada prevista; nula si faltaba una duración necesaria
 */
public record PlanificacionOriginal(UUID pedidoId, Origen origen, String reglas, Instant calculadaEn,
        LocalDate llegada, LocalDate finalizacion, List<EtapaPrevista> etapas) {

    /** Reglas de ADR-0030; se guardan con cada planificación. */
    public static final String REGLAS_VIGENTES = "ESTIMACION_REFERENCIA_1";

    public enum Origen {
        GUARDADA,
        RECONSTRUIDA
    }

    /** Fin previsto de una etapa; nulo si no se podía estimar. */
    public record EtapaPrevista(int numero, LocalDate fin) {
    }

    public PlanificacionOriginal {
        Objects.requireNonNull(pedidoId, "pedidoId");
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(reglas, "reglas");
        etapas = List.copyOf(etapas);
    }

    /** A partir de la previsión calculada solo con los datos de creación ({@code DatosDelPedido.soloCreacion}). */
    public static PlanificacionOriginal desde(UUID pedidoId, Origen origen, Instant calculadaEn,
            Cronograma.Prevision prevision) {
        return new PlanificacionOriginal(pedidoId, origen, REGLAS_VIGENTES, calculadaEn,
                prevision.llegada() == null ? null : prevision.llegada().fecha(), prevision.finalizacionEstimada(),
                prevision.etapas().stream().map(e -> new EtapaPrevista(e.numero(), e.fin())).toList());
    }

    /** Fin previsto de la etapa; nulo si no se podía estimar o no existe. */
    public LocalDate finDe(int numero) {
        return etapas.stream().filter(e -> e.numero() == numero).map(EtapaPrevista::fin).filter(Objects::nonNull)
                .findFirst().orElse(null);
    }
}
