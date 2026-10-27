package com.decokasa.seguimiento.compartido.application.port.out;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Puerto de salida para la fecha y la hora (R5, arquitectura.md). Los plazos se calculan con la hora de Ecuador
 * (ADR-0006); en las pruebas se usa un reloj fijo en lugar del reloj del sistema.
 */
public interface Reloj {

    ZoneId ZONA_ECUADOR = ZoneId.of("America/Guayaquil");

    Instant ahora();

    default ZoneId zona() {
        return ZONA_ECUADOR;
    }

    default ZonedDateTime ahoraEnEcuador() {
        return ahora().atZone(zona());
    }
}
