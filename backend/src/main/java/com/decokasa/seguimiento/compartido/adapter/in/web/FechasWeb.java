package com.decokasa.seguimiento.compartido.adapter.in.web;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Esquema FechaHora del contrato: ISO 8601 con la zona de Ecuador, por ejemplo 2026-10-09T14:15:00-05:00
 * (ADR-0006).
 */
public final class FechasWeb {

    private FechasWeb() {
    }

    /** Nulo si el instante es nulo. */
    public static String texto(Instant instante) {
        return instante == null ? null
                : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instante.atZone(Reloj.ZONA_ECUADOR).toOffsetDateTime());
    }
}
