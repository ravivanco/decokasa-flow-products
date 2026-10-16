package com.decokasa.seguimiento.compartido.domain;

import java.util.Objects;

/**
 * Plazo de una etapa según su definición (columnas definicion_etapa.plazo_*). Distingue tres casos para no inventar
 * valores (decisión del equipo, 2026-10-15): un plazo PENDIENTE del cliente no es "sin plazo" ni un plazo de cero.
 */
public sealed interface PlazoDeEtapa {

    /** El cliente dio el plazo: cantidad y forma de contarlo. */
    record Definido(int cantidad, TipoPlazo tipo) implements PlazoDeEtapa {
        public Definido {
            if (cantidad <= 0) {
                throw new DatoInvalido("Un plazo definido debe ser mayor que cero.");
            }
            Objects.requireNonNull(tipo, "tipo");
        }
    }

    /** La etapa no tiene plazo (A1 "—", Fin). */
    record SinPlazo() implements PlazoDeEtapa {
    }

    /** El cliente todavía no lo definió (⏸). No genera vencimiento. */
    record Pendiente() implements PlazoDeEtapa {
    }
}
