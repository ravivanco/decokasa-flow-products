package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import java.time.Instant;
import java.util.Objects;

/**
 * Urgencia de una devolución por producto nuevo, con su vencimiento propio hasta volver a Análisis (requerimientos
 * 4.2 #11; columnas pedido.urgencia_*). Java puro (R1).
 */
public record UrgenciaActiva(NivelUrgencia nivel, Instant activadaEn, Instant venceEn) {

    public UrgenciaActiva {
        Objects.requireNonNull(nivel, "nivel");
        Objects.requireNonNull(activadaEn, "activadaEn");
        Objects.requireNonNull(venceEn, "venceEn");
        if (!venceEn.isAfter(activadaEn)) {
            throw new DatoInvalido("La urgencia vence después de activarse.");
        }
    }
}
