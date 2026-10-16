package com.decokasa.seguimiento.plazos.domain;

import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Calendario de los plazos (ADR-0006, aceptado por el cliente): días hábiles de lunes a viernes con la hora de Ecuador
 * (America/Guayaquil); una fecha que cae en sábado o domingo pasa al lunes. Java puro (R1).
 *
 * <p>Lo que sigue pendiente del cliente (⏸) y por eso NO se aplica aquí:
 * <ul>
 *   <li>una jornada dentro del día (por ejemplo, de 8:00 a 17:00): no está documentada, así que una hora hábil es
 *       cualquier hora de un día de lunes a viernes;</li>
 *   <li>feriados de Ecuador o de China: sin calendario aprobado, no se descuenta ninguno.</li>
 * </ul>
 * Si el cliente define alguno de los dos, el cambio queda en esta clase.
 */
public final class CalendarioLaboral {

    private final ZoneId zona;

    public CalendarioLaboral(ZoneId zona) {
        this.zona = Objects.requireNonNull(zona, "zona");
    }

    /** El momento en que vence un plazo definido que empieza en {@code inicio}. */
    public Instant vencimiento(Instant inicio, PlazoDeEtapa.Definido plazo) {
        ZonedDateTime desde = inicio.atZone(zona);
        ZonedDateTime vence = switch (plazo.tipo()) {
            case HORAS_HABILES -> sumarHorasHabiles(desde, plazo.cantidad());
            case DIAS_HABILES -> sumarDiasHabiles(desde, plazo.cantidad());
            case DIAS_CORRIDOS -> desde.plusDays(plazo.cantidad());
        };
        return alLunesSiEsFinDeSemana(vence).toInstant();
    }

    /** Solo cuentan las horas de días de lunes a viernes. */
    private ZonedDateTime sumarHorasHabiles(ZonedDateTime desde, int horas) {
        ZonedDateTime momento = desde;
        Duration restante = Duration.ofHours(horas);
        while (!restante.isZero()) {
            if (esFinDeSemana(momento)) {
                momento = momento.toLocalDate().plusDays(1).atStartOfDay(zona);
                continue;
            }
            ZonedDateTime finDelDia = momento.toLocalDate().plusDays(1).atStartOfDay(zona);
            Duration disponible = Duration.between(momento, finDelDia);
            if (disponible.compareTo(restante) >= 0) {
                momento = momento.plus(restante);
                restante = Duration.ZERO;
            } else {
                restante = restante.minus(disponible);
                momento = finDelDia;
            }
        }
        return momento;
    }

    /** Suma días de lunes a viernes y conserva la hora. */
    private ZonedDateTime sumarDiasHabiles(ZonedDateTime desde, int dias) {
        ZonedDateTime momento = desde;
        int sumados = 0;
        while (sumados < dias) {
            momento = momento.plusDays(1);
            if (!esFinDeSemana(momento)) {
                sumados++;
            }
        }
        return momento;
    }

    private ZonedDateTime alLunesSiEsFinDeSemana(ZonedDateTime momento) {
        ZonedDateTime resultado = momento;
        while (esFinDeSemana(resultado)) {
            resultado = resultado.plusDays(1);
        }
        return resultado;
    }

    private static boolean esFinDeSemana(ZonedDateTime momento) {
        DayOfWeek dia = momento.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }
}
