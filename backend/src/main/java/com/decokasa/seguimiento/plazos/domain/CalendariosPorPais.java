package com.decokasa.seguimiento.plazos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Calendario laboral de cada país (fase 7, paso 7.2). Decisión de Richard del 2026-10-22: Colombia usa las mismas
 * reglas provisionales que Ecuador (días hábiles de lunes a viernes, fin de semana al lunes, sin jornada ni feriados
 * porque siguen pendientes del cliente), con la configuración separada por país. Cada país tiene su propia zona, así
 * que un cambio futuro (feriados, jornada) se hace en el país que corresponda sin tocar el otro. Java puro (R1).
 */
public final class CalendariosPorPais {

    private final Map<CodigoPais, CalendarioLaboral> calendarios;

    public CalendariosPorPais(Map<CodigoPais, CalendarioLaboral> calendarios) {
        Objects.requireNonNull(calendarios, "calendarios");
        this.calendarios = calendarios.isEmpty() ? Map.of() : new EnumMap<>(calendarios);
    }

    /**
     * Configuración provisional vigente: Ecuador con la hora de Ecuador (ADR-0006) y Colombia con la de Colombia. Las
     * dos son UTC−5 sin horario de verano, así que hoy dan las mismas fechas; quedan separadas para poder cambiar una
     * sin la otra.
     */
    public static CalendariosPorPais provisionales() {
        return new CalendariosPorPais(Map.of(
                CodigoPais.EC, new CalendarioLaboral(ZoneId.of("America/Guayaquil")),
                CodigoPais.CO, new CalendarioLaboral(ZoneId.of("America/Bogota"))));
    }

    /**
     * El calendario del país.
     *
     * @throws IllegalArgumentException si el país no tiene calendario configurado (nunca se usa el de otro país)
     */
    public CalendarioLaboral de(CodigoPais pais) {
        CalendarioLaboral calendario = pais == null ? null : calendarios.get(pais);
        if (calendario == null) {
            throw new IllegalArgumentException("No hay calendario laboral configurado para el país " + pais + ".");
        }
        return calendario;
    }
}
