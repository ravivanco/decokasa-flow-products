package com.decokasa.seguimiento.pedidos.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Demora vigente en aduana (F4-D5, provisional del equipo, pendiente de confirmación del cliente): la última que se
 * reportó en A7. Las anteriores quedan en el historial (DEMORA_REPORTADA). La fecha es una estimación, no un
 * vencimiento: el plazo de Aduana sigue pendiente (F4-D6). Java puro (R1).
 *
 * @param nuevaFechaEstimadaLlegada la fecha efectiva, ya pasada al lunes si caía en fin de semana
 * @param reportadaEn hora del servidor
 */
public record DemoraAduana(String motivo, LocalDate nuevaFechaEstimadaLlegada, UUID reportadaPor, Instant reportadaEn) {

    public DemoraAduana {
        Objects.requireNonNull(motivo, "motivo");
        Objects.requireNonNull(nuevaFechaEstimadaLlegada, "nuevaFechaEstimadaLlegada");
        Objects.requireNonNull(reportadaPor, "reportadaPor");
        Objects.requireNonNull(reportadaEn, "reportadaEn");
    }
}
